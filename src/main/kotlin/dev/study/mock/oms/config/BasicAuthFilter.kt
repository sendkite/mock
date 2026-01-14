package dev.study.mock.oms.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64

@Component
class BasicAuthFilter(
    @Value("\${oms.auth.username}") private val username: String,
    @Value("\${oms.auth.password}") private val password: String
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // H2 콘솔은 인증 제외
        if (request.requestURI.startsWith("/h2-console")) {
            filterChain.doFilter(request, response)
            return
        }

        // /api 경로만 인증 필요
        if (!request.requestURI.startsWith("/api")) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.setHeader("WWW-Authenticate", "Basic realm=\"OMS API\"")
            response.writer.write("""{"error": "UNAUTHORIZED", "message": "Missing or invalid Authorization header"}""")
            response.contentType = "application/json"
            return
        }

        try {
            val base64Credentials = authHeader.substring("Basic ".length)
            val credentials = String(Base64.getDecoder().decode(base64Credentials))
            val (providedUsername, providedPassword) = credentials.split(":", limit = 2)

            if (providedUsername == username && providedPassword == password) {
                filterChain.doFilter(request, response)
            } else {
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.writer.write("""{"error": "UNAUTHORIZED", "message": "Invalid credentials"}""")
                response.contentType = "application/json"
            }
        } catch (e: Exception) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("""{"error": "UNAUTHORIZED", "message": "Invalid Authorization header format"}""")
            response.contentType = "application/json"
        }
    }
}
