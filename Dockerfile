FROM amazoncorretto:21

WORKDIR /app

# El jar y la carpeta webroot se copian por separado, porque el servidor
# lee webroot del sistema de archivos (Paths.get("webroot")), no del
# classpath empaquetado dentro del jar.
COPY target/webframework-extension.jar app.jar
COPY webroot ./webroot

ENV PORT=8080
ENV APP_ENV=production

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]