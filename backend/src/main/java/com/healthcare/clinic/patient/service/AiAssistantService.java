package com.healthcare.clinic.patient.service;

import com.healthcare.clinic.identity.entity.User;
import com.healthcare.clinic.patient.entity.AiChatMessage;
import com.healthcare.clinic.patient.entity.AiChatSession;
import com.healthcare.clinic.patient.entity.PatientProfile;
import com.healthcare.clinic.patient.repository.AiChatMessageRepository;
import com.healthcare.clinic.patient.repository.AiChatSessionRepository;
import com.healthcare.clinic.patient.repository.PatientProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final PatientProfileRepository patientProfileRepository;

    private PatientProfile getPatientProfile(User user) {
        return patientProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found for user"));
    }

    @Transactional
    public AiChatSession getOrCreateActiveSession(User user) {
        PatientProfile profile = getPatientProfile(user);
        return sessionRepository.findByPatientId(profile.getId())
                .stream().findFirst()
                .orElseGet(() -> {
                    AiChatSession newSession = AiChatSession.builder()
                            .patientId(profile.getId())
                            .build();
                    return sessionRepository.save(newSession);
                });
    }

    public List<AiChatMessage> getSessionMessages(User user, Long sessionId) {
        return messageRepository.findBySessionIdOrderBySentAtAsc(sessionId);
    }

    @Transactional
    public AiChatMessage sendMessage(User user, Long sessionId, String content) {
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Save User Message
        AiChatMessage userMessage = AiChatMessage.builder()
                .session(session)
                .senderType("USER")
                .content(content)
                .build();
        messageRepository.save(userMessage);

        String aiResponseText = "Thank you for reaching out to the AI Health Assistant. Your request: \"" + content + "\" is being processed.";

        // Save AI Message
        AiChatMessage aiMessage = AiChatMessage.builder()
                .session(session)
                .senderType("AI")
                .content(aiResponseText)
                .build();
        return messageRepository.save(aiMessage);
    }
}
