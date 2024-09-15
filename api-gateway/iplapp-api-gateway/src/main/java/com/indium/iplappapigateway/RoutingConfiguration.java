package com.indium.iplappapigateway;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingConfiguration {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route for uploading match data
                .route("uploadMatchData", r -> r.path("/api/matches/upload")
//                    .uri("lb://ipl-assignment"))
                        .uri("http://localhost:5000"))
                // Route for fetching wickets by player
                .route("wicketsByPlayer", r -> r.path("/api/matches/wickets-by-player")
                        .uri("http://localhost:5000"))

                // Route for fetching top wicket takers
                .route("topWicketTakers", r -> r.path("/api/matches/top-wicket-takers")
                        .uri("http://localhost:5000"))

                // Route for fetching top batsmen
                .route("topBatsmen", r -> r.path("/api/matches/top-batsmen")
                        .uri("http://localhost:5000"))

                // Route for fetching strike rate by batter and match
                .route("strikeRate", r -> r.path("/api/matches/strike-rate")
                        .uri("http://localhost:5000"))

                // Route for fetching players by team and match
                .route("playersByTeamMatch", r -> r.path("/api/matches/players-by-team-match")
                        .uri("http://localhost:5000"))

                // Route for fetching match referees by match number
                .route("matchReferees", r -> r.path("/api/matches/match-referees")
                        .uri("http://localhost:5000"))

                // Route for fetching inning scores by date
                .route("inningScoresByDate", r -> r.path("/api/matches/inning-scores-by-date")
                        .uri("http://localhost:5000"))

                // Route for fetching cumulative score by player
                .route("cumulativeScoreByPlayer", r -> r.path("/api/matches/cumulative-score-by-player")
                        .uri("http://localhost:5000"))

                // Route for fetching matches by player
                .route("matchesByPlayer", r -> r.path("/api/matches/get-matches-by-player")
                        .uri("http://localhost:5000"))
                .build();
    }
}
