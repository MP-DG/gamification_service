package de.unistuttgart.iste.meitrex.gamification_service.client;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class QueryDefinitions {

    public static final String PLAYER_HEXAD_SCORE_BY_ID_QUERY = """
        query($userId: UUID!) {
            _internal_noauth_getPlayerHexadScoreById(userId: $userId) {
                scores {
                    type 
                    value
                }
            }
        }
        """;

    public static final String PLAYER_HEXAD_SCORE_BY_ID_NAME = "_internal_noauth_getPlayerHexadScoreById";

}
