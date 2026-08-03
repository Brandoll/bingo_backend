package com.bsplay.room.presentation.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class RoomApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void createsRoomJoinsPlayerAndReturnsSnapshot() throws Exception {
        String body = mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomName":"Noche de prueba","hostName":"Ana","maxPlayers":12}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("HOST"))
                .andExpect(jsonPath("$.room.members.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(body);
        String code = created.path("room").path("code").asText();
        String hostToken = created.path("token").asText();

        String playerBody = mockMvc.perform(post("/api/v1/rooms/{code}/join", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Luis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.room.members.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        String playerToken = objectMapper.readTree(playerBody).path("token").asText();
        String playerId = objectMapper.readTree(playerBody).path("memberId").asText();

        mockMvc.perform(patch("/api/v1/rooms/{code}/settings", code)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardsPerPlayer":2,"allowLateJoin":true,"hideParticipantNames":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsPerPlayer").value(2))
                .andExpect(jsonPath("$.hideParticipantNames").value(true));

        mockMvc.perform(patch("/api/v1/rooms/{code}/members/{memberId}/co-host", code, playerId)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[1].role").value("CO_HOST"));

        mockMvc.perform(post("/api/v1/rooms/{code}/start", code)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        mockMvc.perform(get("/api/v1/rooms/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Noche de prueba"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.members[1].displayName").value("Luis"));

        mockMvc.perform(get("/api/v1/rooms/{code}/game", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.remainingNumbers").value(90))
                .andExpect(jsonPath("$.automaticBingoDetectionEnabled").value(false))
                .andExpect(jsonPath("$.stopOnBingoEnabled").value(true))
                .andExpect(jsonPath("$.winnerAnnouncementEnabled").value(true));

        mockMvc.perform(patch("/api/v1/rooms/{code}/game/settings", code)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lineEnabled":true,"doubleLineEnabled":true,"bingoEnabled":true,
                                 "rankingPublic":true,"automaticBingoDetectionEnabled":true,
                                 "stopOnBingoEnabled":false,"winnerAnnouncementEnabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.automaticBingoDetectionEnabled").value(true))
                .andExpect(jsonPath("$.stopOnBingoEnabled").value(false))
                .andExpect(jsonPath("$.winnerAnnouncementEnabled").value(false));

        mockMvc.perform(get("/api/v1/rooms/{code}/game/cards/me", code)
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].grid.length()").value(3));

        mockMvc.perform(get("/api/v1/rooms/{code}/game/cards/me", code)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].grid.length()").value(3));

        mockMvc.perform(post("/api/v1/rooms/{code}/game/draws", code)
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drawnNumbers.length()").value(1))
                .andExpect(jsonPath("$.remainingNumbers").value(89));

        mockMvc.perform(post("/api/v1/rooms/{code}/game/pause", code)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(post("/api/v1/rooms/{code}/game/draws", code)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_NOT_RUNNING"));

        mockMvc.perform(post("/api/v1/rooms/{code}/game/resume", code)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        mockMvc.perform(post("/api/v1/rooms/{code}/game/finish", code)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ROUND_FINISHED"));

        mockMvc.perform(post("/api/v1/rooms/{code}/close", code)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.locked").value(true));

        mockMvc.perform(get("/api/v1/rooms/{code}/game", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void detectsDigitalBingoStopsRoundAndPublishesWinner() throws Exception {
        String createdBody = mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomName":"Bingo automático","hostName":"Marta","maxPlayers":8}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createdBody);
        String code = created.path("room").path("code").asText();
        String hostToken = created.path("token").asText();

        mockMvc.perform(post("/api/v1/rooms/{code}/start", code)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rooms/{code}/game/settings", code)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lineEnabled":true,"doubleLineEnabled":true,"bingoEnabled":true,
                                 "rankingPublic":true,"automaticBingoDetectionEnabled":true,
                                 "stopOnBingoEnabled":true,"winnerAnnouncementEnabled":true}
                                """))
                .andExpect(status().isOk());

        JsonNode snapshot = null;
        for (int draw = 0; draw < 90; draw++) {
            String drawBody = mockMvc.perform(post("/api/v1/rooms/{code}/game/draws", code)
                            .header("Authorization", "Bearer " + hostToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            snapshot = objectMapper.readTree(drawBody);
            if ("ROUND_FINISHED".equals(snapshot.path("status").asText())) break;
        }

        org.junit.jupiter.api.Assertions.assertNotNull(snapshot);
        org.junit.jupiter.api.Assertions.assertEquals("ROUND_FINISHED", snapshot.path("status").asText());
        org.junit.jupiter.api.Assertions.assertTrue(snapshot.path("claims").findValuesAsText("prizeType").contains("BINGO"));
        org.junit.jupiter.api.Assertions.assertTrue(snapshot.path("claims").findValuesAsText("status").contains("APPROVED"));
    }
}
