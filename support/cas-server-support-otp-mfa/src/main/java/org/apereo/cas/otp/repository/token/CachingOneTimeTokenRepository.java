package org.apereo.cas.otp.repository.token;

import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This is {@link CachingOneTimeTokenRepository}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
public class CachingOneTimeTokenRepository extends BaseOneTimeTokenRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachingOneTimeTokenRepository.class);
    
    private final LoadingCache<String, Collection<OneTimeToken>> storage;

    public CachingOneTimeTokenRepository(final LoadingCache<String, Collection<OneTimeToken>> storage) {
        this.storage = storage;
    }

    @Override
    public void cleanInternal() {
        LOGGER.debug("Beginning to clean up the cache storage to remove expiring tokens");
        this.storage.cleanUp();
        LOGGER.debug("Total of [{}] token(s) remain in the cache and may be removed in future iterations", this.storage.size());
    }

    @Override
    public void store(final OneTimeToken token) {
        if (exists(token.getUserId(), token.getToken())) {
            try {
                final Collection<OneTimeToken> tokens = this.storage.get(token.getUserId());
                tokens.add(token);

                LOGGER.debug("Storing previously used tokens [[{}]] for user [{}]", tokens, token.getUserId());
                this.storage.put(token.getUserId(), tokens);
            } catch (final Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }
        } else {
            final Collection<OneTimeToken> tokens = new ArrayList<>();
            tokens.add(token);

            LOGGER.debug("Storing previously used token [{}] for user [{}]", token, token.getUserId());
            this.storage.put(token.getUserId(), tokens);
        }
    }

    @Override
    public boolean exists(final String uid, final Integer otp) {
        try {
            final Collection<OneTimeToken> tokens = this.storage.getIfPresent(uid);
            LOGGER.debug("Found used tokens [{}]", tokens);
            return tokens != null && tokens.stream().anyMatch(t -> t.getToken().equals(otp));
        } catch (final Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }
        return false;
    }
}
