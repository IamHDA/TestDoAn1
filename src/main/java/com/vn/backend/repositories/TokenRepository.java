package com.vn.backend.repositories;

import com.vn.backend.entities.Token;
import com.vn.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByRefreshTokenAndIsRevokedFalse(String refreshToken);


    @Modifying
    @Transactional
    @Query("UPDATE Token t SET t.isRevoked = true WHERE t.user = :user")
    void revokeAllUserTokens(@Param("user") User user);


    @Modifying
    @Transactional
    @Query("UPDATE Token t SET t.isRevoked = true WHERE t.refreshToken = :refreshToken")
    void revokeRefreshToken(@Param("refreshToken") String refreshToken);


}
