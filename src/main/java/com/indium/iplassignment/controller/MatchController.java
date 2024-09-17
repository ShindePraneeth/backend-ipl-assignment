package com.indium.iplassignment.controller;

import com.indium.iplassignment.entity.Official;
import com.indium.iplassignment.entity.Player;
import com.indium.iplassignment.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

//import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Cricket Match API", description = "API for managing cricket matches and statistics")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Method to log actions to Kafka
    private void sendLogToKafka(String action, Object details) {
        try {
            Map<String, Object> log = new HashMap<>();
            log.put("action", action);
            log.put("details", details);
            String logMessage = objectMapper.writeValueAsString(log);
            kafkaTemplate.send("match-logs-topic", logMessage);
        } catch (Exception e) {
            e.printStackTrace(); // Handle logging error
        }
    }

    @Operation(summary = "Upload match data", description = "Upload a JSON file containing match data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Match data uploaded and saved successfully"),
            @ApiResponse(responseCode = "409", description = "Match data already exists in the database", content = @Content),
            @ApiResponse(responseCode = "400", description = "File is empty or failed to parse JSON file", content = @Content),
            @ApiResponse(responseCode = "500", description = "Failed to save match data", content = @Content)
    })
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadMatchData(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            sendLogToKafka("Upload match data failed", "File is empty");
            return new ResponseEntity<>("File is empty", HttpStatus.BAD_REQUEST);
        }

        try {
            JsonNode matchData = objectMapper.readTree(file.getInputStream());

            boolean isMatchSaved = matchService.saveMatchData(matchData);
            if (!isMatchSaved) {
                sendLogToKafka("Upload match data conflict", "Match data already exists in the database");
                return new ResponseEntity<>("Match data already exists in the database", HttpStatus.CONFLICT);
            }

            sendLogToKafka("Upload match data success", matchData.toString());
            return new ResponseEntity<>("Match data uploaded and saved successfully", HttpStatus.OK);
        } catch (IOException e) {
            sendLogToKafka("Upload match data failed", "Failed to parse JSON file");
            return new ResponseEntity<>("Failed to parse JSON file", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            sendLogToKafka("Upload match data failed", "Failed to save match data: " + e.getMessage());
            return new ResponseEntity<>("Failed to save match data: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Get matches by player")
    @ApiResponse(responseCode = "200", description = "List of matches returned")
    @GetMapping("/get-matches-by-player")
    public ResponseEntity<List<Map<String, Object>>> getMatchesByPlayer(@RequestParam(required = true) String playerName, Authentication authentication) {
        if (playerName == null || playerName.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<Map<String, Object>> matchEvents = matchService.getMatchEventsByPlayerName(playerName);
        sendLogToKafka("Get matches by player", "Player: " + playerName + ", Result count: " + matchEvents.size());

        if (matchEvents.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(matchEvents, HttpStatus.OK);
    }

    @Operation(summary = "Get wickets taken by a player")
    @ApiResponse(responseCode = "200", description = "List of wickets returned")
    @GetMapping("/wickets-by-player")
    public ResponseEntity<List<Map<String, Object>>> getWicketsByPlayer(@RequestParam String playerName, Authentication authentication) {
        if (playerName == null || playerName.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<Map<String, Object>> wickets = matchService.getWicketsByBowler(playerName);
        sendLogToKafka("Get wickets by player", "Player: " + playerName + ", Wickets count: " + wickets.size());

        return new ResponseEntity<>(wickets, HttpStatus.OK);
    }

    @Operation(summary = "Get cumulative score by player")
    @ApiResponse(responseCode = "200", description = "Cumulative score returned")
    @GetMapping("/cumulative-score-by-player")
    public ResponseEntity<String> getCumulativeScoreByPlayer(@RequestParam(required = true) String playerName, Authentication authentication) {
        if (playerName == null || playerName.isEmpty()) {
            sendLogToKafka("Get cumulative score by player failed", "Missing required playerName parameter");
            return new ResponseEntity<>("Missing required playerName parameter", HttpStatus.BAD_REQUEST);
        }

        Integer cumulativeScore = matchService.getCumulativeScoreByBatter(playerName);
        sendLogToKafka("Get cumulative score by player", "Player: " + playerName + ", Cumulative score: " + cumulativeScore);

        return new ResponseEntity<>(String.format("Cumulative score for %s is %d", playerName, cumulativeScore), HttpStatus.OK);
    }

    @Operation(summary = "Get inning scores by date")
    @ApiResponse(responseCode = "200", description = "List of inning scores returned")
    @GetMapping("/inning-scores-by-date")
    public ResponseEntity<List<Map<String, Object>>> getInningScoresByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate matchDate, Authentication authentication) {
        if (matchDate == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<Map<String, Object>> scores = matchService.getInningScoresByDate(matchDate);
        sendLogToKafka("Get inning scores by date", "Match Date: " + matchDate + ", Scores count: " + scores.size());

        if (scores.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(scores, HttpStatus.OK);
    }

    @Operation(summary = "Get players by team and match")
    @ApiResponse(responseCode = "200", description = "List of players returned")
    @GetMapping("/players-by-team-match")
    public ResponseEntity<List<Player>> getPlayersByTeamAndMatch(@RequestParam String teamName, @RequestParam int matchNumber, Authentication authentication) {
        if (teamName == null || teamName.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<Player> players = matchService.getPlayersByTeamAndMatch(teamName, matchNumber);
        sendLogToKafka("Get players by team and match", "Team: " + teamName + ", Match Number: " + matchNumber + ", Players count: " + players.size());

        if (players.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(players, HttpStatus.OK);
    }

    @Operation(summary = "Get match referees by match number")
    @ApiResponse(responseCode = "200", description = "List of referee names returned")
    @GetMapping("/match-referees")
    public ResponseEntity<List<String>> getMatchRefereesByMatchNumber(@RequestParam Long matchNumber, Authentication authentication) {
        if (matchNumber == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<Official> matchReferees = matchService.getMatchRefereesByMatchNumber(matchNumber);
        List<String> refereeNames = matchReferees.stream()
                .map(Official::getOfficialName)
                .collect(Collectors.toList());

        sendLogToKafka("Get match referees by match number", "Match Number: " + matchNumber + ", Referees count: " + refereeNames.size());

        return ResponseEntity.ok(refereeNames);
    }

    @Operation(summary = "Get top batsmen")
    @ApiResponse(responseCode = "200", description = "List of top batsmen returned")
    @GetMapping("/top-batsmen")
    public ResponseEntity<List<Object[]>> getTopBatsmen(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size);
        List<Object[]> topBatsmen = matchService.getTopBatsmen(pageable);

        sendLogToKafka("Get top batsmen", "Page: " + page + ", Size: " + size + ", Batsmen count: " + topBatsmen.size());

        return ResponseEntity.ok(topBatsmen);
    }

    @Operation(summary = "Get strike rate by batter and match")
    @ApiResponse(responseCode = "200", description = "Strike rate returned")
    @GetMapping("/strike-rate")
    public ResponseEntity<String> getStrikeRateByBatterAndMatch(@RequestParam String batterName, @RequestParam int matchNumber, Authentication authentication) {
        if (batterName == null || batterName.isEmpty()) {
            return new ResponseEntity<>("Batter name is required", HttpStatus.BAD_REQUEST);
        }

        String result = matchService.getStrikeRateByBatterAndMatch(batterName, matchNumber);

        if (result.contains("No data found")) {
            sendLogToKafka("Get strike rate failed", "Batter: " + batterName + ", Match Number: " + matchNumber + ", Result: No data found");
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        } else {
            sendLogToKafka("Get strike rate success", "Batter: " + batterName + ", Match Number: " + matchNumber + ", Strike Rate: " + result);
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }

    @Operation(summary = "Get top wicket takers")
    @ApiResponse(responseCode = "200", description = "List of top wicket takers returned")
    @GetMapping("/top-wicket-takers")
    public ResponseEntity<List<Object[]>> getTopWicketTakers(@RequestParam int page, @RequestParam int size, Authentication authentication) {
        List<Object[]> wicketTakers = matchService.getTopWicketTakers(page, size);

        sendLogToKafka("Get top wicket takers", "Page: " + page + ", Size: " + size + ", Wicket Takers count: " + wicketTakers.size());

        if (wicketTakers.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(wicketTakers, HttpStatus.OK);
    }
}
