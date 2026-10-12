package br.com.loja.repository;

import br.com.loja.dto.LimitDecision;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthenticationLimitRepository {
  private final JdbcTemplate jdbcTemplate;

  public LimitDecision consume(
      String scope, String keyHash, int maximum, Instant now, Instant expires) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO authentication_limit(scope,key_hash,attempts,window_end) VALUES(?,?,1,?)
        ON CONFLICT(scope,key_hash) DO UPDATE SET
         attempts=CASE WHEN authentication_limit.window_end<=? THEN 1 ELSE LEAST(authentication_limit.attempts+1,?+1) END,
         window_end=CASE WHEN authentication_limit.window_end<=? THEN EXCLUDED.window_end ELSE authentication_limit.window_end END
        RETURNING attempts,window_end
        """,
        (result, row) ->
            new LimitDecision(
                result.getInt("attempts") <= maximum,
                Math.max(
                    1,
                    result.getTimestamp("window_end").toInstant().getEpochSecond()
                        - now.getEpochSecond())),
        scope,
        keyHash,
        Timestamp.from(expires),
        Timestamp.from(now),
        maximum,
        Timestamp.from(now));
  }

  public void cleanExpired(Instant now) {
    jdbcTemplate.update(
        "DELETE FROM authentication_limit WHERE window_end<? AND (scope,key_hash) IN (SELECT"
            + " scope,key_hash FROM authentication_limit WHERE window_end<? LIMIT 1000)",
        Timestamp.from(now),
        Timestamp.from(now));
  }
}
