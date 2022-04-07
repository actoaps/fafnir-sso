package dk.acto.fafnir.server.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.io.BaseEncoding;
import dk.acto.fafnir.api.exception.*;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.crypto.RsaKeyManager;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Component
@AllArgsConstructor
public class TokenFactory {
	private final RsaKeyManager keyManager;
	public String generateToken (final UserData ud, OrganisationData od, ClaimData cd, ProviderMetaData pmd) {
		var jwt = JWT.create();

		var userData = Optional.ofNullable(ud)
				.orElseThrow(NoUser::new);

		var orgData = Optional.ofNullable(od)
						.orElseThrow(NoOrganisation::new);

		var claimData = Optional.ofNullable(cd)
						.orElseThrow(NoClaimData::new);

		jwt.withIssuer(Optional.ofNullable(pmd)
						.map(ProviderMetaData::getProviderId)
				.map(idp -> "fafnir-" + idp)
				.orElseThrow(NoIssuer::new));

		jwt.withSubject(Optional.ofNullable(userData.getSubject())
				.orElseThrow(NoSubject::new));

		jwt.withIssuedAt(Date.from(Instant.now()));

		Optional.ofNullable(userData.getName())
				.ifPresent(name -> jwt.withClaim("name", name));

		Optional.ofNullable(userData.getMetaId())
				.ifPresent(name -> jwt.withClaim("mId", name));

		Optional.ofNullable(userData.getLocale())
				.ifPresent(locale -> jwt.withClaim("locale", locale.toLanguageTag()));

		Optional.ofNullable(orgData.getOrganisationId())
				.ifPresent(orgId -> jwt.withClaim("org_id", orgId));

		Optional.ofNullable(orgData.getOrganisationName())
				.ifPresent(orgName -> jwt.withClaim("org_name", orgName));

		Optional.ofNullable(claimData.getClaims())
				.filter(x -> x.length > 0)
				.ifPresent(roles -> jwt.withArrayClaim("role", roles));

		return Try.of(() -> Algorithm.RSA512(keyManager.getPublicKey(), keyManager.getPrivateKey()))
				.map(jwt::sign)
				.get();
	}
}
