package com.indium.iplassignment;
import com.fasterxml.jackson.databind.JsonNode;
import com.indium.iplassignment.controller.MatchController;
import com.indium.iplassignment.service.MatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.Collections;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(MatchController.class)
public class MatchControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;
    @Test
    public void testUploadMatchDataSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "matches.json", "application/json", "{\"matchId\":1}".getBytes());

        when(matchService.saveMatchData(any(JsonNode.class))).thenReturn(true);

        mockMvc.perform(multipart("/api/matches/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("Match data uploaded and saved successfully"));

        verify(matchService, times(1)).saveMatchData(any(JsonNode.class));
    }

    @Test
    public void testUploadMatchDataConflict() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "matches.json", "application/json", "{\"matchId\":1}".getBytes());

        when(matchService.saveMatchData(any(JsonNode.class))).thenReturn(false);

        mockMvc.perform(multipart("/api/matches/upload")
                        .file(file))
                .andExpect(status().isConflict())
                .andExpect(content().string("Match data already exists in the database"));

        verify(matchService, times(1)).saveMatchData(any(JsonNode.class));
    }

    @Test
    public void testUploadMatchDataEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "matches.json", "application/json", new byte[0]);

        mockMvc.perform(multipart("/api/matches/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("File is empty"));

        verify(matchService, never()).saveMatchData(any(JsonNode.class));
    }

    @Test
    public void testGetInningScoresByDate() throws Exception {
        LocalDate matchDate = LocalDate.of(2023, 8, 30);

        when(matchService.getInningScoresByDate(matchDate)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/matches/inning-scores-by-date")
                        .param("matchDate", "2023-08-30"))
                .andExpect(status().isNoContent());

        verify(matchService, times(1)).getInningScoresByDate(matchDate);
    }

    @Test
    public void testGetPlayersByTeamAndMatch() throws Exception {
        String teamName = "Team A";
        int matchNumber = 5;

        when(matchService.getPlayersByTeamAndMatch(teamName, matchNumber)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/matches/players-by-team-match")
                        .param("teamName", teamName)
                        .param("matchNumber", String.valueOf(matchNumber)))
                .andExpect(status().isNoContent());

        verify(matchService, times(1)).getPlayersByTeamAndMatch(teamName, matchNumber);
    }

    @Test
    public void testGetMatchRefereesByMatchNumber() throws Exception {
        Long matchNumber = 10L;

        when(matchService.getMatchRefereesByMatchNumber(matchNumber)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/matches/match-referees")
                        .param("matchNumber", String.valueOf(matchNumber)))
                .andExpect(status().isOk())  // Expect 200 OK instead of 204 No Content
                .andExpect(content().json("[]"));  // Verify that the response is an empty array

        verify(matchService, times(1)).getMatchRefereesByMatchNumber(matchNumber);
    }


    @Test
    public void testGetTopBatsmen() throws Exception {
        when(matchService.getTopBatsmen(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/matches/top-batsmen")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())  // Change from isNoContent() to isOk()
                .andExpect(content().json("[]"));  // Expect an empty array as the response body

        verify(matchService, times(1)).getTopBatsmen(any());
    }


    @Test
    public void testGetTopWicketTakers() throws Exception {
        when(matchService.getTopWicketTakers(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/matches/top-wicket-takers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isNoContent());


        verify(matchService, times(1)).getTopWicketTakers(anyInt(), anyInt());
    }
    @Test
    public void testGetMatchesByPlayer() throws Exception {
        when(matchService.getMatchEventsByPlayerName("Player A")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/matches/get-matches-by-player")
                        .param("playerName", "Player A"))
                .andExpect(status().isNoContent());

        verify(matchService, times(1)).getMatchEventsByPlayerName("Player A");
    }
    @Test
    public void testGetWicketsByPlayer() throws Exception {
        when(matchService.getWicketsByBowler("Player A")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/matches/wickets-by-player")
                        .param("playerName", "Player A"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(matchService, times(1)).getWicketsByBowler("Player A");
    }
    @Test
    public void testGetCumulativeScoreByPlayerSuccess() throws Exception {
        when(matchService.getCumulativeScoreByBatter("Player A")).thenReturn(100);

        mockMvc.perform(get("/api/matches/cumulative-score-by-player")
                        .param("playerName", "Player A"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cumulative score for Player A is 100"));

        verify(matchService, times(1)).getCumulativeScoreByBatter("Player A");
    }

    @Test
    public void testGetCumulativeScoreByPlayerMissingParameter() throws Exception {
        mockMvc.perform(get("/api/matches/cumulative-score-by-player"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing required playerName parameter"));

        verify(matchService, never()).getCumulativeScoreByBatter(anyString());
    }
    @Test
    public void testGetStrikeRateByBatterAndMatchSuccess() throws Exception {
        when(matchService.getStrikeRateByBatterAndMatch("Player A", 1)).thenReturn("Strike rate for Player A in match number 1 is 75.00");

        mockMvc.perform(get("/api/matches/strike-rate")
                        .param("batterName", "Player A")
                        .param("matchNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Strike rate for Player A in match number 1 is 75.00"));

        verify(matchService, times(1)).getStrikeRateByBatterAndMatch("Player A", 1);
    }

    @Test
    public void testGetStrikeRateByBatterAndMatchNotFound() throws Exception {
        when(matchService.getStrikeRateByBatterAndMatch("Player A", 1)).thenReturn("No data found");

        mockMvc.perform(get("/api/matches/strike-rate")
                        .param("batterName", "Player A")
                        .param("matchNumber", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No data found"));

        verify(matchService, times(1)).getStrikeRateByBatterAndMatch("Player A", 1);
    }

}
