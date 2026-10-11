package br.com.loja.security;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
class RequestBodyLimitFilterTest {
 @Test void rejectsOversizeIncludingWithoutDeclaredLength() throws Exception {
  var request=new MockHttpServletRequest("POST","/api/v1/auth/customer/login"); request.setContentType("application/json");request.setContent(new byte[262145]);
  var response=new MockHttpServletResponse();new RequestBodyLimitFilter(262144).doFilter(request,response,(incoming,outgoing)->fail("must not pass"));assertEquals(413,response.getStatus());
 }
 @Test void permitsExactLimitAndPreservesBody() throws Exception {
  var request=new MockHttpServletRequest("POST","/api/v1/auth/customer/login");request.setContentType("application/json");request.setContent(new byte[262144]);
  var response=new MockHttpServletResponse();new RequestBodyLimitFilter(262144).doFilter(request,response,(incoming,outgoing)->assertEquals(262144,incoming.getInputStream().readAllBytes().length));assertEquals(200,response.getStatus());
 }
}
