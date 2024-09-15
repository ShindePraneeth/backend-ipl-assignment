package com.indium.iplassignment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indium.iplassignment.entity.Official;
import com.indium.iplassignment.entity.Player;
import com.indium.iplassignment.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc

public class MatchControllerIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private TestRestTemplate testRestTemplate;
    @MockBean
    private MatchService matchService;

    @BeforeEach
    public void setup() throws JsonProcessingException {
        // Mock the service behavior instead of adding real data
        // Mock the ObjectMapper reading a file
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode mockMatchData = objectMapper.readTree("{ \"matchNumber\": 100, \"team1\": \"Team A\", \"team2\": \"Team B\" }");

        // Mock behavior for successful match data saving
        Mockito.when(matchService.saveMatchData(Mockito.any(JsonNode.class))).thenReturn(true);

        // Mock behavior when match data already exists
        Mockito.when(matchService.saveMatchData(Mockito.eq(mockMatchData))).thenReturn(false);
        Mockito.when(matchService.getMatchEventsByPlayerName("Sachin Tendulkar"))
                .thenReturn(Collections.singletonList(Map.of("match", "Match 1", "score", "100")));

        // Mock the behavior for getWicketsByBowler
        Mockito.when(matchService.getWicketsByBowler("Anil Kumble"))
                .thenReturn(Collections.singletonList(Map.of("match", "Match 2", "wickets", 5)));

        // Mock the behavior for getCumulativeScoreByBatter
        Mockito.when(matchService.getCumulativeScoreByBatter("Rahul Dravid"))
                .thenReturn(500);
        Mockito.when(matchService.getMatchEventsByPlayerName("Sachin Tendulkar"))
                .thenReturn(Collections.singletonList(Map.of("match", "Match 1", "score", "100")));

        Mockito.when(matchService.getWicketsByBowler("Anil Kumble"))
                .thenReturn(Collections.singletonList(Map.of("match", "Match 2", "wickets", 5)));

        Official official1 = new Official();
        official1.setOfficialName("Referee 1");

        Official official2 = new Official();
        official2.setOfficialName("Referee 2");

        List<Official> mockOfficials = List.of(official1, official2);

        // Mock the service call
        Mockito.when(matchService.getMatchRefereesByMatchNumber(100L))
                .thenReturn(mockOfficials);

        Mockito.when(matchService.getMatchRefereesByMatchNumber(100L))
                .thenReturn(mockOfficials);


        Mockito.when(matchService.getTopWicketTakers(0, 5))
                .thenReturn(List.of(
                        new Object[]{"Anil Kumble", 5},
                        new Object[]{"Shane Warne", 4}
                ));
        Mockito.when(matchService.getStrikeRateByBatterAndMatch("Virender Sehwag", 5))
                .thenReturn("Strike Rate: 120.5");
        Mockito.when(matchService.getTopBatsmen(Mockito.any(Pageable.class)))
                .thenReturn(List.of(
                        new Object[]{"BB McCullum", 163},
                        new Object[]{"A Symonds", 149},
                        new Object[]{"KC Sangakkara", 148},
                        new Object[]{"G Gambhir", 144},
                        new Object[]{"GC Smith", 120}
                ));
        Player player1 = new Player();
        player1.setPlayerName("Player 1");

        Player player2 = new Player();
        player2.setPlayerName("Player 2");

        List<Player> mockPlayers = List.of(player1, player2);

        // Mock the service behavior for this specific team and match
        Mockito.when(matchService.getPlayersByTeamAndMatch("Team A", 1))
                .thenReturn(mockPlayers);
        Mockito.when(matchService.getPlayersByTeamAndMatch("Team B", 2))
                .thenReturn(Collections.emptyList());
        Map<String, Object> inningScore1 = Map.of(
                "team", "Team A",
                "score", 250
        );

        Map<String, Object> inningScore2 = Map.of(
                "team", "Team B",
                "score", 230
        );

        List<Map<String, Object>> mockScores = List.of(inningScore1, inningScore2);

        // Mocking the behavior of the matchService for the specific date
        LocalDate matchDate = LocalDate.of(2024, 9, 14);
        Mockito.when(matchService.getInningScoresByDate(matchDate))
                .thenReturn(mockScores);
    }
    @Test
    public void testUploadMatchDataSuccess() throws Exception {
        String url = "http://localhost:" + port + "/api/matches/upload";

        // Create a mock file to upload
        ClassPathResource fileResource = new ClassPathResource("test-match-data.json");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        // Set the headers for multipart/form-data
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Call the API
        ResponseEntity<String> response = testRestTemplate.postForEntity(url, requestEntity, String.class);

        // Assert that the response is OK and the message is as expected
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Match data uploaded and saved successfully"));
    }


    @Test
    public void testGetWicketsByPlayer() {
        String playerName = "Anil Kumble";
        String url = "http://localhost:" + port + "/api/matches/wickets-by-player?playerName=" + playerName;

        ResponseEntity<List> response = testRestTemplate.getForEntity(url, List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    public void testGetCumulativeScoreByPlayer() {
        String playerName = "Rahul Dravid";
        String url = "http://localhost:" + port + "/api/matches/cumulative-score-by-player?playerName=" + playerName;

        ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Cumulative score for Rahul Dravid"));
    }


    @Test
    public void testGetTopBatsmen() {
        Pageable pageable = PageRequest.of(0, 5);  // Page 0, size 5
        List<Object[]> topBatsmen = matchService.getTopBatsmen(pageable);

        assertEquals(5, topBatsmen.size());
        assertEquals("BB McCullum", topBatsmen.get(0)[0]);
        assertEquals(163, topBatsmen.get(0)[1]);
        assertEquals("A Symonds", topBatsmen.get(1)[0]);
        assertEquals(149, topBatsmen.get(1)[1]);
    }


    @Test
    public void testGetStrikeRateByBatterAndMatch() {
        String batterName = "Virender Sehwag";
        int matchNumber = 5;
        String url = "http://localhost:" + port + "/api/matches/strike-rate?batterName=" + batterName + "&matchNumber=" + matchNumber;

        ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);

        System.out.println("Response Body: " + response.getBody());  // Log response body

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Strike Rate"));  // Ensure this matches the actual response
    }


    @Test
    public void testGetTopWicketTakers() {
        String url = "http://localhost:" + port + "/api/matches/top-wicket-takers?page=0&size=5";

        ResponseEntity<List> response = testRestTemplate.getForEntity(url, List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
    }
    @Test
    public void testGetMatchRefereesByMatchNumber() {
        String url = "http://localhost:" + port + "/api/matches/match-referees?matchNumber=100";
        ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Referee 1"));
        assertTrue(response.getBody().contains("Referee 2"));
    }

    @Test
    public void testGetPlayersByTeamAndMatch() {
        String url = "http://localhost:" + port + "/api/matches/players-by-team-match?teamName=Team A&matchNumber=1";
        ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Player 1"));
        assertTrue(response.getBody().contains("Player 2"));
    }


    @Test
    public void testGetInningScoresByDate() {
        String url = "http://localhost:" + port + "/api/matches/inning-scores-by-date?matchDate=2024-09-14";
        ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Team A"));
        assertTrue(response.getBody().contains("Team B"));
        assertTrue(response.getBody().contains("250"));
        assertTrue(response.getBody().contains("230"));
    }



}
