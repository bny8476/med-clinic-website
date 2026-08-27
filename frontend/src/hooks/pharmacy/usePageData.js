import { useQuery } from '@tanstack/react-query';
import { fetchWithRetry } from '../../utils/pharmacy/api';
import { useSearchParams } from 'react-router-dom';
import { useRef, useEffect } from 'react';

/**
 * Reusable hook for paginated data fetching
 * @param {string} queryKey - Unique key for the query
 * @param {string} endpoint - API endpoint to fetch from
 * @param {Object} extraParams - Additional query parameters
 */
export const usePageData = (queryKey, endpoint, extraParams = {}) => {
  const [searchParams, setSearchParams] = useSearchParams();
  
  const page = parseInt(searchParams.get('page') || '0', 10);
  const size = parseInt(searchParams.get('size') || '20', 10);

  const query = useQuery({
    queryKey: [queryKey, page, size, extraParams],
    queryFn: () => fetchWithRetry(endpoint, {
      params: { page, size, ...extraParams }
    }).then(res => {
      // Handle both: Spring Page wrapper (ApiResponse.data = {content,totalElements,...})
      // and flat list (ApiResponse.data = [...])
      const payload = res.data?.data ?? res.data;
      return payload;
    }),
    staleTime: 5000,
    placeholderData: (prev) => prev,
  });

  const lastDataRef = useRef(null);
  useEffect(() => {
    if (query.data !== undefined) lastDataRef.current = query.data;
  }, [query.data]);

  const activeData = query.data ?? lastDataRef.current;

  const goToPage = (newPage) => {
    setSearchParams(prev => {
      prev.set('page', newPage);
      return prev;
    });
  };

  // Normalise: if backend returned a Spring Page, use .content. If plain array, use as-is.
  const isPage = activeData && !Array.isArray(activeData) && Array.isArray(activeData.content);

  return {
    ...query,
    isLoading: query.isPending,
    isFetching: query.isFetching,
    isError: query.isError,
    page,
    size,
    goToPage,
    totalPages: isPage ? (activeData.totalPages ?? 0) : 0,
    totalElements: isPage ? (activeData.totalElements ?? 0) : (Array.isArray(activeData) ? activeData.length : 0),
    items: isPage ? activeData.content : (Array.isArray(activeData) ? activeData : []),
  };
};
