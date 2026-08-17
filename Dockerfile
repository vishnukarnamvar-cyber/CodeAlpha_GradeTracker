FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY Student.java .
COPY StudentGradeServer.java .

COPY public ./public

RUN javac Student.java StudentGradeServer.java

EXPOSE 10000

CMD ["java", "StudentGradeServer"]
