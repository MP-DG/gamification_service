package de.unistuttgart.iste.meitrex.gamification_service.client;

import de.unistuttgart.iste.meitrex.gamification_service.exception.GamificationServiceConnectionException;
import de.unistuttgart.iste.meitrex.generated.dto.PlayerHexadScore;
import de.unistuttgart.iste.meitrex.generated.dto.PlayerTypeScore;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClient;
import org.modelmapper.Converter;
import reactor.core.publisher.SynchronousSink;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


/**
 * Client for the gamification service, allowing to query contents
 */
@Slf4j
public class GamificationServiceClient {

    private static final long RETRY_COUNT = 3;
    private final GraphQlClient graphQlClient;
    private final ModelMapper modelMapper;

    public GamificationServiceClient(final GraphQlClient graphQlClient) {
        this.graphQlClient = graphQlClient;
        this.modelMapper = new ModelMapper();
        modelMapper.typeMap(String.class, OffsetDateTime.class).setConverter(stringToOffsetDateTimeConverter());
    }

    public PlayerHexadScore getPlayerHexadScoreById (final UUID userID) throws GamificationServiceConnectionException {
        log.info("Querying player hexad score for user with the id {}", userID);

        try {
            return graphQlClient.document(QueryDefinitions.PLAYER_HEXAD_SCORE_BY_ID_QUERY)
                    .variable("userID", userID)
                    .execute()
                    .handle((ClientGraphQlResponse result, SynchronousSink<PlayerHexadScore> sink)
                            -> handleGamificationServiceResponse(result, sink, QueryDefinitions.PLAYER_HEXAD_SCORE_BY_ID_NAME))
                    .retry(RETRY_COUNT)
                    .block();
        } catch (final RuntimeException e) {
            unwrapContentServiceConnectionException(e);
        }
        return null; // unreachable
    }

    private void handleGamificationServiceResponse(final ClientGraphQlResponse result,
                                                   final SynchronousSink<PlayerHexadScore> sink,
                                                   final String queryName) {
        log.info(result.toString());
        if (!result.isValid()) {
            sink.error(new GamificationServiceConnectionException(
                    "Error while fetching contents from content service: Invalid response.",
                    result.getErrors()));
            return;
        }

        final PlayerHexadScore retrievedContents;
        try {
            retrievedContents = convertResponseToListOfPlayerTypeScore(result, queryName);
        } catch (final GamificationServiceConnectionException e) {
            sink.error(e);
            return;
        }

        sink.next(retrievedContents);
    }

    /**
     * @return query response
     * @throws GamificationServiceConnectionException if something went wrong
     */
    private PlayerHexadScore convertResponseToListOfPlayerTypeScore(ClientGraphQlResponse result, String queryName) throws GamificationServiceConnectionException{
        return result.field(queryName + "[0]").toEntity(PlayerHexadScore.class);
    }

    /**
     *
     */
    private static void unwrapContentServiceConnectionException(final RuntimeException e) throws GamificationServiceConnectionException {
        // block wraps exceptions in a RuntimeException, so we need to unwrap them
        if (e.getCause() instanceof final GamificationServiceConnectionException gamificationServiceConnectionException) {
            throw gamificationServiceConnectionException;
        }
        // if the exception is not a GamificationServiceConnectionException, we don't know how to handle it
        throw e;
    }

    private Converter<String, OffsetDateTime> stringToOffsetDateTimeConverter() {
        return context -> {
            final String source = context.getSource();
            if (source == null) {
                return null;
            }
            return OffsetDateTime.parse(source);
        };
    }
}
