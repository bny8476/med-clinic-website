import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * Decodes a JWT payload segment from base64url to a plain object.
 *
 * WHY: JWT payloads use base64url encoding (RFC 4648 §5) which replaces
 * '+' → '-' and '/' → '_' and omits '=' padding. The browser's native
 * `atob()` only understands standard base64 and throws InvalidCharacterError
 * on any token whose raw payload bytes map to '+' or '/' characters.
 * This function normalises the alphabet and restores padding before decoding.
 *
 * NOTE — SECURITY TRADEOFF (localStorage):
 * The token is persisted via Zustand's `persist` middleware to localStorage.
 * This is convenient but means an XSS vulnerability could steal the token.
 * The alternative is an httpOnly cookie set by the backend, which is
 * inaccessible to JavaScript and therefore XSS-resistant. We accept
 * localStorage here because: (a) the frontend has a strict CSP, (b) the
 * access-token lifetime is short (15 minutes), and (c) implementing
 * cookie-based auth would require CSRF protection as a new trade-off.
 * If XSS posture changes, migrate to httpOnly cookies on the backend.
 */
export function parseJwtPayload(token) {
  const base64url = token.split('.')[1];
  if (!base64url) throw new Error('Malformed JWT: missing payload segment');

  // Normalise base64url → standard base64
  const base64 = base64url
    .replace(/-/g, '+')
    .replace(/_/g, '/')
    .padEnd(base64url.length + (4 - (base64url.length % 4)) % 4, '=');

  try {
    return JSON.parse(atob(base64));
  } catch {
    throw new Error('Malformed JWT: payload is not valid base64url');
  }
}

/**
 * Returns true only if the JWT token exists and hasn't expired yet.
 * Gives a 30-second buffer to account for clock skew.
 */
export function isTokenValid(token) {
  if (!token) return false;
  try {
    const { exp } = parseJwtPayload(token);
    if (!exp) return false;
    return (exp * 1000) > (Date.now() + 30_000); // 30-sec buffer
  } catch {
    return false;
  }
}

const extractErrorMessage = (err, fallback) => {
  if (err.response?.data) {
    if (typeof err.response.data === 'string') return err.response.data;
    if (err.response.data.message) return err.response.data.message;
  }
  if (err.code === 'ECONNABORTED') {
    return 'Server cold start timed out. The backend is waking up, please try again in a moment.';
  }
  if (err.message === 'Network Error' || !err.response) {
    return 'Server unreachable. Please check backend status or CORS configuration.';
  }
  return fallback;
};

const useAuthStore = create(
    persist(
        (set) => ({
            token: null,
            user: null,
            roles: [],
            mfaPending: false,
            mfaEmail: null,
            error: null,
            isLoading: false,

            login: async (portal, email, password) => {
                set({ isLoading: true, error: null });
                try {
                    // Avoid circular dependency by dynamically importing axiosPublic
                    const { axiosPublic } = await import('../api/axios');
                    const res = await axiosPublic.post(`/auth/${portal}/login`, { email, password });
                    
                    if (res.data.mfaRequired) {
                        set({ mfaPending: true, mfaEmail: res.data.email, isLoading: false });
                        return false; // MFA needed
                    }
                    
                    const { token } = res.data;
                    const parsedToken = parseJwtPayload(token);
                    set({ 
                        token, 
                        roles: parsedToken.roles || [],
                        user: { id: parsedToken.userId, email: parsedToken.sub },
                        mfaPending: false,
                        isLoading: false 
                    });
                    return true;
                } catch (err) {
                    set({ error: extractErrorMessage(err, 'Login failed. Please check credentials.'), isLoading: false });
                    return false;
                }
            },
            
            verifyMfa: async (portal, email, otp) => {
                set({ isLoading: true, error: null });
                try {
                    const { axiosPublic } = await import('../api/axios');
                    const res = await axiosPublic.post(`/auth/${portal}/login/mfa`, { email, otp });
                    
                    const { token } = res.data;
                    const parsedToken = parseJwtPayload(token);
                    set({ 
                        token, 
                        roles: parsedToken.roles || [],
                        user: { id: parsedToken.userId, email: parsedToken.sub },
                        mfaPending: false,
                        mfaEmail: null,
                        isLoading: false 
                    });
                    return true;
                } catch (err) {
                    set({ error: extractErrorMessage(err, 'Invalid OTP or expired code'), isLoading: false });
                    return false;
                }
            },

            refresh: async () => {
                try {
                    const { axiosPublic } = await import('../api/axios');
                    // Must include withCredentials to send the HttpOnly refresh_token cookie
                    const res = await axiosPublic.post(`/auth/refresh`, {}, { withCredentials: true });
                    
                    const newAccessToken = res.data.accessToken;
                    
                    set({ token: newAccessToken });
                    return newAccessToken;
                } catch (_err) {
                    return null;
                }
            },

            forgotPassword: async (email) => {
                set({ isLoading: true, error: null });
                try {
                    const { axiosPublic } = await import('../api/axios');
                    await axiosPublic.post('/auth/password/forgot', { email });
                    set({ isLoading: false });
                    return true;
                } catch (err) {
                    set({ error: extractErrorMessage(err, 'Failed to send reset code'), isLoading: false });
                    return false;
                }
            },

            resetPassword: async (email, otp, newPassword) => {
                set({ isLoading: true, error: null });
                try {
                    const { axiosPublic } = await import('../api/axios');
                    await axiosPublic.post('/auth/password/reset', { email, otp, newPassword });
                    set({ isLoading: false });
                    return true;
                } catch (err) {
                    set({ error: err.response?.data || 'Failed to reset password', isLoading: false });
                    return false;
                }
            },

            clearError: () => set({ error: null }),
            logout: async () => {
                set({ token: null, user: null, roles: [], mfaPending: false, mfaEmail: null, error: null });
                try {
                    const { axiosPrivate } = await import('../api/axios');
                    await axiosPrivate.post('/auth/logout');
                } catch (err) {
                    // Ignore errors if backend session is already dead or network fails
                }
            },
            // Clears any stale/expired token from storage without full logout UI
            clearStaleToken: () => {
                const { token } = useAuthStore.getState();
                if (token && !isTokenValid(token)) {
                    set({ token: null, user: null, roles: [] });
                }
            },
            isAuthenticated: () => {
                const { token } = useAuthStore.getState();
                return isTokenValid(token);
            },
            hasRole: (role) => useAuthStore.getState().roles.includes(role),
        }),
        {
            name: 'auth-storage',
            partialize: (state) => ({
                token: state.token,
                user: state.user,
                roles: state.roles,
            }),
        }
    )
);

export default useAuthStore;
