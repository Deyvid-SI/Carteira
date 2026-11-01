package br.com.carteira.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "br.com.carteira.controller.api")
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail notFound(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail business(RegraNegocioException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage(), request);
    }

    @ExceptionHandler(ServicoExternoException.class)
    ProblemDetail external(ServicoExternoException ex, HttpServletRequest request) {
        ProblemDetail detail = problem(HttpStatus.BAD_GATEWAY, "Falha em serviço externo", ex.getMessage(), request);
        detail.setProperty("servico", ex.getServico());
        return detail;
    }

    /** Mesma mensagem para senha errada, conta inexistente ou desativada, sem revelar quais e-mails existem. */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail authentication(AuthenticationException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Não autenticado", "E-mail ou senha inválidos.", request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ProblemDetail validation(Exception ex, HttpServletRequest request) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Revise os campos informados.", request);
        if (ex instanceof MethodArgumentNotValidException manv) {
            Map<String, String> errors = new LinkedHashMap<>();
            manv.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
            detail.setProperty("erros", errors);
        }
        return detail;
    }

    private ProblemDetail problem(HttpStatus status, String title, String message, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(title);
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
