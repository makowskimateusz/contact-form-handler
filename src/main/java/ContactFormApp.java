import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ContactFormApp extends AbstractVerticle {

    private static final String MAILGUN_AUTH = System.getProperty("MAILGUN_AUTH");
    private static final String MAILGUN_ID = System.getProperty("MAILGUN_ID");
    private static final String RECAPTCHA_SECRET = System.getProperty("RECAPTCHA_SECRET");

    public static void main(String[] args) throws UnsupportedEncodingException {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new ContactFormApp());
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.route().handler(StaticHandler.create("makowskimateusz.github.io"));

        router.post("/contact-form-send").handler(ctx -> {
            String name = ctx.request().getFormAttribute("Name");
            String email = ctx.request().getFormAttribute("EmailAddress");
            String text = ctx.request().getFormAttribute("textarea");
            String recaptchaToken = ctx.request().getFormAttribute("g-recaptcha-response");



            vertx.createHttpClient(new HttpClientOptions().setSsl(true))
                    .post(443, "www.google.com", "/recaptcha/api/siteverify")
                    .putHeader("Content-Type", "application/x-www-form-urlencoded")
                    .handler(resp -> resp.bodyHandler(body -> {
                        JsonObject verifyResponse = new JsonObject(body.toString());
                        boolean success = verifyResponse.getBoolean("success");

                        if (success) {
                            sendEmail(name, email);
                        }
                    }))
                    .end("response=" + recaptchaToken + "&secret=" + RECAPTCHA_SECRET);

            ctx.response().setStatusCode(301).putHeader("Location", "index.html").end();
        });

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080);
    }

    private void sendEmail(String name, String email) {
        try {
            vertx.createHttpClient(new HttpClientOptions().setSsl(true))
                    .post(443, "api.mailgun.net", "/v3/" + MAILGUN_ID + "/messages")
                    .putHeader("Authorization", "Basic " + MAILGUN_AUTH)
                    .putHeader("Content-Type", "application/x-www-form-urlencoded")
                    .handler(resp -> resp.bodyHandler(b -> System.out.println((b.toString()))))
                    .end("from=" + URLEncoder.encode(name + " <" + email + ">", "UTF-8") + "&to=" + URLEncoder.encode("krzmak@gmail.com", "UTF-8") + "&text=testMaila");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
