package com.indium.iplassignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indium.iplassignment.entity.*;
import com.indium.iplassignment.repository.*;
import com.indium.iplassignment.service.MatchService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchServiceUnitTest {

    @InjectMocks
    private MatchService matchService;

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private OfficialRepository officialRepository;
    @Mock
    private InningRepository inningRepository;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private OutcomeRepository outcomeRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Transactional
    void testSaveMatchData_NewMatch_Success() throws Exception {
        // Prepare the JSON data with two teams and players
        String jsonString = "{\"info\":{\"event\":{\"match_number\":1,\"name\":\"IPL 2023\"},"
                + "\"match_type\":\"T20\",\"city\":\"Mumbai\",\"venue\":\"Wankhede Stadium\","
                + "\"toss\":{\"winner\":\"Team A\",\"decision\":\"bat\"},"
                + "\"player_of_match\":[\"Player X\"],\"outcome\":{\"winner\":\"Team A\"},"
                + "\"dates\":[\"2023-04-01\"],\"players\":{\"Team A\":[\"Player 1\", \"Player 2\"],"
                + "\"Team B\":[\"Player 3\", \"Player 4\"]},"
                + "\"officials\":{\"umpire\":[\"Umpire 1\", \"Umpire 2\"]}}}"; // Ensure two teams are present

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode matchData = objectMapper.readTree(jsonString);

        // Mock repository behavior
        when(matchRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Execute the service method
        boolean result = matchService.saveMatchData(matchData);

        // Assert the result
        assertTrue(result);

        // Verify repositories interactions
        verify(matchRepository, times(1)).save(any(Match.class)); // Ensure match was saved
        verify(teamRepository, times(1)).saveAll(anyList()); // Ensure teams were saved once
        verify(playerRepository, times(1)).saveAll(anyList()); // Ensure players were saved once
        verify(officialRepository, times(1)).saveAll(anyList()); // Ensure officials were saved
        verify(outcomeRepository, times(1)).save(any(Outcome.class)); // Ensure outcome was saved
    }

    @Test
    @Transactional
    void testSaveMatchData_MatchExists() throws Exception {
        // Prepare the JSON data
        String jsonString = "{\"info\":{\"event\":{\"match_number\":1,\"name\":\"IPL 2023\"},"
                + "\"match_type\":\"T20\",\"city\":\"Mumbai\",\"venue\":\"Wankhede Stadium\","
                + "\"toss\":{\"winner\":\"Team A\",\"decision\":\"bat\"},"
                + "\"player_of_match\":[\"Player X\"],\"outcome\":{\"winner\":\"Team A\"},"
                + "\"dates\":[\"2023-04-01\"],\"players\":{\"Team A\":[\"Player 1\", \"Player 2\"],"
                + "\"Team B\":[\"Player 3\", \"Player 4\"]},\"officials\":{\"umpire\":[\"Umpire 1\", \"Umpire 2\"]},\"innings\":[]}}";

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode matchData = objectMapper.readTree(jsonString);

        // Mock repository behavior to return an existing match
        when(matchRepository.findById(anyLong())).thenReturn(Optional.of(new Match()));

        // Call the service
        boolean result = matchService.saveMatchData(matchData);

        // Verify the results
        assertFalse(result);
        verify(matchRepository, never()).save(any(Match.class));
    }
    @Test
    void testSaveMatchData_MissingInningsData() throws Exception {
        // Prepare the JSON data without innings
        String jsonString = "{\"info\":{\"event\":{\"match_number\":1,\"name\":\"IPL 2023\"},"
                + "\"match_type\":\"T20\",\"city\":\"Mumbai\",\"venue\":\"Wankhede Stadium\","
                + "\"toss\":{\"winner\":\"Team A\",\"decision\":\"bat\"},"
                + "\"player_of_match\":[\"Player X\"],\"outcome\":{\"winner\":\"Team A\"},"
                + "\"dates\":[\"2023-04-01\"],\"players\":{\"Team A\":[\"Player 1\", \"Player 2\"],"
                + "\"Team B\":[\"Player 3\", \"Player 4\"]},"
                + "\"officials\":{\"umpire\":[\"Umpire 1\", \"Umpire 2\"]}}}";// No innings data

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode matchData = objectMapper.readTree(jsonString);

        // Mock repository behavior
        when(matchRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Call the service
        boolean result = matchService.saveMatchData(matchData);

        // Verify the results
        assertTrue(result);
        verify(matchRepository, times(1)).save(any(Match.class));
        verify(teamRepository, times(1)).saveAll(anyList()); // Adjusted to expect one batch save for teams
        verify(playerRepository, times(1)).saveAll(anyList());
        verify(officialRepository, times(1)).saveAll(anyList());
        verify(outcomeRepository, times(1)).save(any(Outcome.class));

        // Ensure that parseInnings doesn't attempt to process innings since it's missing
        verify(inningRepository, never()).save(any(Inning.class));
        verify(deliveryRepository, never()).saveAll(anyList());
    }
    @Test
    void testGetMatchEventsByPlayerName() {
        // Prepare mock data from repository
        List<Object[]> mockData = new ArrayList<>();
        mockData.add(new Object[]{1L, "T20", "Mumbai", "Wankhede Stadium", "IPL 2023", "Team A", "Team B", "bat", "Player X", LocalDate.of(2023, 4, 1)});
        mockData.add(new Object[]{2L, "ODI", "Delhi", "Feroz Shah Kotla", "World Cup", "Team B", "Team C", "field", "Player Y", LocalDate.of(2023, 5, 10)});

        // Mock repository behavior
        String playerName = "Player X";
        when(matchRepository.findMatchEventsByPlayerName(playerName)).thenReturn(mockData);

        // Call the service
        List<Map<String, Object>> result = matchService.getMatchEventsByPlayerName(playerName);

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());

        // Validate first result
        Map<String, Object> firstMatch = result.get(0);
        assertEquals(1L, firstMatch.get("match_number"));
        assertEquals("T20", firstMatch.get("match_type"));
        assertEquals("Mumbai", firstMatch.get("city"));
        assertEquals("Wankhede Stadium", firstMatch.get("venue"));
        assertEquals("IPL 2023", firstMatch.get("event_name"));
        assertEquals("Team A", firstMatch.get("winner"));
        assertEquals("Team B", firstMatch.get("toss_winner"));
        assertEquals("bat", firstMatch.get("toss_decision"));
        assertEquals("Player X", firstMatch.get("player_of_match"));
        assertEquals(LocalDate.of(2023, 4, 1), firstMatch.get("match_date"));

        // Validate second result
        Map<String, Object> secondMatch = result.get(1);
        assertEquals(2L, secondMatch.get("match_number"));
        assertEquals("ODI", secondMatch.get("match_type"));
        assertEquals("Delhi", secondMatch.get("city"));
        assertEquals("Feroz Shah Kotla", secondMatch.get("venue"));
        assertEquals("World Cup", secondMatch.get("event_name"));
        assertEquals("Team B", secondMatch.get("winner"));
        assertEquals("Team C", secondMatch.get("toss_winner"));
        assertEquals("field", secondMatch.get("toss_decision"));
        assertEquals("Player Y", secondMatch.get("player_of_match"));
        assertEquals(LocalDate.of(2023, 5, 10), secondMatch.get("match_date"));

        // Verify repository interaction
        verify(matchRepository, times(1)).findMatchEventsByPlayerName(playerName);
    }
    @Test
    void testGetWicketsByBowler() {
        // Prepare mock data
        List<Object[]> mockData = new ArrayList<>();
        mockData.add(new Object[]{1L, 10, 3, true, "Batsman 1", "Bowler 1", "bowled"});
        mockData.add(new Object[]{2L, 12, 5, true, "Batsman 2", "Bowler 1", "caught"});

        // Mock repository behavior
        String playerName = "Bowler 1";
        when(deliveryRepository.findWicketsByBowler(playerName)).thenReturn(mockData);

        // Call the service
        List<Map<String, Object>> result = matchService.getWicketsByBowler(playerName);

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());

        // Validate first wicket details
        Map<String, Object> firstWicket = result.get(0);
        assertEquals(1L, firstWicket.get("deliveryId"));
        assertEquals(10, firstWicket.get("over"));
        assertEquals(3, firstWicket.get("ball"));
        assertTrue((Boolean) firstWicket.get("wicket"));
        assertEquals("Batsman 1", firstWicket.get("batsman"));
        assertEquals("Bowler 1", firstWicket.get("bowler"));
        assertEquals("bowled", firstWicket.get("dismissalType"));

        // Validate second wicket details
        Map<String, Object> secondWicket = result.get(1);
        assertEquals(2L, secondWicket.get("deliveryId"));
        assertEquals(12, secondWicket.get("over"));
        assertEquals(5, secondWicket.get("ball"));
        assertTrue((Boolean) secondWicket.get("wicket"));
        assertEquals("Batsman 2", secondWicket.get("batsman"));
        assertEquals("Bowler 1", secondWicket.get("bowler"));
        assertEquals("caught", secondWicket.get("dismissalType"));

        // Verify repository interaction
        verify(deliveryRepository, times(1)).findWicketsByBowler(playerName);
    }

    @Test
    void testGetInningScoresByDate() {
        // Prepare mock data from repository
        List<Object[]> mockData = new ArrayList<>();
        mockData.add(new Object[]{1L, 101L, "Team A", 150});
        mockData.add(new Object[]{1L, 102L, "Team B", 140});

        // Mock repository behavior
        LocalDate matchDate = LocalDate.of(2023, 4, 1);
        when(deliveryRepository.findInningScoresByDate(matchDate)).thenReturn(mockData);

        // Call the method
        List<Map<String, Object>> result = matchService.getInningScoresByDate(matchDate);

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());

        // Validate first result
        Map<String, Object> firstScore = result.get(0);
        assertEquals(1L, firstScore.get("matchNumber"));
        assertEquals(101L, firstScore.get("inningId"));
        assertEquals("Team A", firstScore.get("battingTeam"));
        assertEquals(150, firstScore.get("totalScore"));

        // Validate second result
        Map<String, Object> secondScore = result.get(1);
        assertEquals(1L, secondScore.get("matchNumber"));
        assertEquals(102L, secondScore.get("inningId"));
        assertEquals("Team B", secondScore.get("battingTeam"));
        assertEquals(140, secondScore.get("totalScore"));

        // Verify repository interaction
        verify(deliveryRepository, times(1)).findInningScoresByDate(matchDate);
    }


    @Test
    void testGetCumulativeScoreByBatter() {
        // Prepare test data
        String batterName = "Batsman A";
        Integer mockScore = 100;

        // Mock repository behavior
        when(deliveryRepository.getCumulativeScoreByBatter(batterName)).thenReturn(mockScore);

        // Execute the method
        Integer result = matchService.getCumulativeScoreByBatter(batterName);

        // Verify the result
        assertEquals(mockScore, result);
    }

    @Test
    void testGetPlayersByTeamAndMatch() {
        // Prepare test data
        String teamName = "Team A";
        int matchNumber = 1;
        List<Player> mockPlayers = Arrays.asList(new Player(), new Player());

        // Mock repository behavior
        when(playerRepository.findPlayersByTeamAndMatch(teamName, matchNumber)).thenReturn(mockPlayers);

        // Execute the method
        List<Player> result = matchService.getPlayersByTeamAndMatch(teamName, matchNumber);

        // Verify the result
        assertEquals(2, result.size());
    }

    @Test
    void testGetMatchRefereesByMatchNumber() {
        // Prepare test data
        Long matchNumber = 1L;
        List<Official> mockOfficials = Arrays.asList(new Official(), new Official());

        // Mock repository behavior
        when(officialRepository.findMatchRefereesByMatchNumber(matchNumber)).thenReturn(mockOfficials);

        // Execute the method
        List<Official> result = matchService.getMatchRefereesByMatchNumber(matchNumber);

        // Verify the result
        assertEquals(2, result.size());
    }

    @Test
    void testGetTopBatsmen() {
        // Prepare test data
        Pageable pageable = PageRequest.of(0, 10);
        List<Object[]> mockData = Arrays.asList(
                new Object[]{"Batsman A", 500},
                new Object[]{"Batsman B", 450}
        );

        // Mock repository behavior
        when(deliveryRepository.findTopBatsmen(pageable)).thenReturn(mockData);

        // Execute the method
        List<Object[]> result = matchService.getTopBatsmen(pageable);

        // Verify the result
        assertEquals(2, result.size());
        assertEquals("Batsman A", result.get(0)[0]);
        assertEquals(500, result.get(0)[1]);
    }

    @Test
    void testGetStrikeRateByBatterAndMatch() {
        // Prepare test data
        String batterName = "Batsman A";
        int matchNumber = 1;
        Double mockStrikeRate = 150.0;

        // Mock repository behavior
        when(deliveryRepository.getStrikeRateByBatterAndMatch(batterName, matchNumber)).thenReturn(mockStrikeRate);

        // Execute the method
        String result = matchService.getStrikeRateByBatterAndMatch(batterName, matchNumber);

        // Verify the result
        assertTrue(result.contains("150.00"));
    }

    @Test
    void testGetTopWicketTakers() {
        // Prepare test data
        int page = 0;
        int size = 10;
        List<Object[]> mockData = Arrays.asList(
                new Object[]{"Bowler A", 20},
                new Object[]{"Bowler B", 18}
        );
        Page<Object[]> mockPage = new PageImpl<>(mockData);

        // Mock repository behavior
        when(deliveryRepository.findTopWicketTakers(any(Pageable.class))).thenReturn(mockPage);

        // Execute the method
        List<Object[]> result = matchService.getTopWicketTakers(page, size);

        // Verify the result
        assertEquals(2, result.size());
        assertEquals("Bowler A", result.get(0)[0]);
        assertEquals(20, result.get(0)[1]);
    }
}
