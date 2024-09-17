package com.indium.iplassignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.indium.iplassignment.controller.MatchController;
import com.indium.iplassignment.jwt.JwtUtil;
import com.indium.iplassignment.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
@Import(NoSecurityConfig.class)
public class MatchControllerUnitTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private MatchService matchService;
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;
    @MockBean
    private Authentication authentication;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testUploadMatchDataSuccess() throws Exception {        // Use valid JSON content for the test
        String jsonContent = "{\"matchId\": 123, \"teamA\": \"Team A\", \"teamB\": \"Team B\"}";
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", jsonContent.getBytes());
        when(matchService.saveMatchData(any(JsonNode.class))).thenReturn(true);
        mockMvc.perform(multipart("/api/matches/upload").file(file).with(csrf()).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testUploadMatchDataConflict() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile("file", "matches.json", MediaType.APPLICATION_JSON_VALUE, "{\"matchNumber\":10}".getBytes());
        when(matchService.saveMatchData(any(JsonNode.class))).thenReturn(false);
        mockMvc.perform(multipart("/api/matches/upload").file(mockFile).accept(MediaType.APPLICATION_JSON)).andExpect(status().isConflict()).andExpect(content().string("Match data already exists in the database"));
        verify(kafkaTemplate).send(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testUploadMatchDataEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        mockMvc.perform(multipart("/api/matches/upload").file(file).with(csrf()).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetCumulativeScoreByPlayerSuccess() throws Exception {
        when(matchService.getCumulativeScoreByBatter("Player A")).thenReturn(100);
        mockMvc.perform(get("/api/matches/cumulative-score-by-player").param("playerName", "Player A").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().string("Cumulative score for Player A is 100"));
        verify(kafkaTemplate).send(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetStrikeRateByBatterAndMatchSuccess() throws Exception {
        when(matchService.getStrikeRateByBatterAndMatch("Player A", 1)).thenReturn("Strike rate for Player A in match number 1 is 75.00");
        mockMvc.perform(get("/api/matches/strike-rate").param("batterName", "Player A").param("matchNumber", "1")).andExpect(status().isOk()).andExpect(content().string("Strike rate for Player A in match number 1 is 75.00"));
        verify(matchService, times(1)).getStrikeRateByBatterAndMatch("Player A", 1);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetStrikeRateByBatterAndMatchNotFound() throws Exception {
        when(matchService.getStrikeRateByBatterAndMatch("Player A", 1)).thenReturn("No data found");
        mockMvc.perform(get("/api/matches/strike-rate").param("batterName", "Player A").param("matchNumber", "1")).andExpect(status().isNotFound()).andExpect(content().string("No data found"));
        verify(matchService, times(1)).getStrikeRateByBatterAndMatch("Player A", 1);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetMatchRefereesByMatchNumber() throws Exception {
        Long matchNumber = 10L;
        when(matchService.getMatchRefereesByMatchNumber(matchNumber)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/matches/match-referees").param("matchNumber", String.valueOf(matchNumber))).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(matchService, times(1)).getMatchRefereesByMatchNumber(matchNumber);
    }
    }