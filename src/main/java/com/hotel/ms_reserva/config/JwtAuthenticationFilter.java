package com.hotel.ms_reserva.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 3. Buscamos el token en los headers ("Authorization")
        String authHeader = request.getHeader("Authorization");

        // 4. Se comprueba si el usuario envió el token y si empieza con Bearer
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 5. Se usa jwtUtil para validar si el token es real (fijate lo limpio que queda)
            if (jwtUtil.isValid(token)) {
                String username = jwtUtil.getUsername(token);

                // 6. Si es válido, le decimos a Spring Security que lo deje pasar
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // Si el token fue manipulado o expiró, limpiamos la seguridad por precaución
                SecurityContextHolder.clearContext();
            }
        }

        // 7. Continúa el flujo normal de la petición hacia el controlador
        filterChain.doFilter(request, response);
    }
}