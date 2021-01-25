package dk.acto.fafnir.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.io.BaseEncoding;
import dk.acto.fafnir.api.exception.NoIssuer;
import dk.acto.fafnir.api.exception.NoSubject;
import dk.acto.fafnir.api.exception.NoUser;
import dk.acto.fafnir.api.model.FafnirUser;
import io.vavr.control.Try;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class TokenFactory {

	private final KeyPair keys;

	public TokenFactory() {
		this.keys = Try.of(() -> KeyPairGenerator.getInstance("RSA"))
				.andThen(x -> x.initialize(1024, new SecureRandom()))
				.map(KeyPairGenerator::generateKeyPair)
				.get();
	}

	public String generateToken (final FafnirUser fafnirUser) {
		var jwt = JWT.create();

		var temp = Optional.ofNullable(fafnirUser)
				.orElseThrow(NoUser::new);

		jwt.withIssuer(Optional.ofNullable(temp.getProvider())
				.map(idp -> "fafnir-" + idp)
				.orElseThrow(NoIssuer::new));

		jwt.withSubject(Optional.ofNullable(temp.getSubject())
				.orElseThrow(NoSubject::new));

		jwt.withIssuedAt(Date.from(Instant.now()));

		Optional.ofNullable(temp.getName())
				.ifPresent(name -> jwt.withClaim("name", name));

		Optional.ofNullable(temp.getMetaId())
				.ifPresent(name -> jwt.withClaim("mId", name));

		Optional.ofNullable(temp.getLocale())
				.ifPresent(locale -> jwt.withClaim("locale", locale.toLanguageTag()));

		Optional.ofNullable(temp.getOrganisationId())
				.ifPresent(orgId -> jwt.withClaim("org_id", orgId));

		Optional.ofNullable(temp.getOrganisationName())
				.ifPresent(orgName -> jwt.withClaim("org_name", orgName));

		Optional.ofNullable(temp.getRoles())
				.filter(x -> !x.isEmpty())
				.ifPresent(roles -> jwt.withArrayClaim("role", roles.toArray(String[]::new)));

		return Try.of(() -> Algorithm.RSA512((RSAPublicKey) keys.getPublic(), (RSAPrivateKey) keys.getPrivate()))
				.map(jwt::sign)
				.get();
	}

	public String getPublicKey() {
		return BaseEncoding.base64().omitPadding().encode(
				keys.getPublic().getEncoded()
		);
	}


	public PrivateKey getPrivateKey() {
		return keys.getPrivate();
	}
}