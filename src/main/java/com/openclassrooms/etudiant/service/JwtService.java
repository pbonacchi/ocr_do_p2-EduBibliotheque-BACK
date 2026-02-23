package com.openclassrooms.etudiant.service;


import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private long expiration;
 
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Générer le token JWT en utilisant le username de l'utilisateur
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSignInKey())
                .compact();
    }

    // Récupérer le username à partir du token JWT
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey()).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public long getExpiration() {
        return expiration;
    }

    public void validateToken(String token) throws JwtException {
        try {
            Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch(JwtException e){
            // catch null, wrong token, expired token
            throw new JwtException(e.getMessage());
        }
    }
}
