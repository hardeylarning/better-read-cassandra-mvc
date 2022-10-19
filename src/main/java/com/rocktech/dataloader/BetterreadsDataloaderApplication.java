package com.rocktech.dataloader;

import com.rocktech.dataloader.author.Author;
import com.rocktech.dataloader.author.AuthorRepository;
import com.rocktech.dataloader.book.Book;
import com.rocktech.dataloader.book.BookRepository;
import connection.DataStaxAstraProperties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraProperties.class)
public class BetterreadsDataloaderApplication {

    @Autowired
    AuthorRepository authorRepository;

    @Autowired
    BookRepository bookRepository;

    @Value("${datadump.location.author}")
    private String authorDumpLocation;

    @Value("${datadump.location.works}")
    private String worksDumpLocation;

    public static void main(String[] args) {

        SpringApplication.run(BetterreadsDataloaderApplication.class, args);
    }

    private void initAuthors() {
        Path path = Paths.get(authorDumpLocation);
        int i = 0;
        try (Stream<String> lines = Files.lines(path)){
            lines.forEach(line -> {
                // Read and parse the line

                String jsonString = line.substring(line.indexOf("{"));
                JSONObject jsonObject = null;

                try {
                    jsonObject = new JSONObject(jsonString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Construct Book object
                Author author = new Author();
                author.setName(jsonObject.optString("name"));
                author.setPersonalName(jsonObject.optString("name"));
                author.setId(jsonObject.optString("key").replace("/authors/", ""));

                // Persist using Repository
                authorRepository.save(author);
                System.out.println("Name: "+author.getName());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initWorks() {
        Path path = Paths.get(worksDumpLocation);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        try (Stream<String> lines = Files.lines(path)) {

            lines.forEach(line -> {
                // Read and parse the line
                String jsonString = line.substring(line.indexOf("{"));
                JSONObject jsonObject = null;

                try {
                    jsonObject = new JSONObject(jsonString);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }

                // Construct Book object
                Book book = new Book();
                assert jsonObject != null;
                book.setId(jsonObject.optString("key").replace("/works/", ""));
                book.setName(jsonObject.optString("title"));

                JSONArray coversArr = jsonObject.optJSONArray("covers");
                if (coversArr != null) {
                    List<String> coverIds = new ArrayList<>();
                    for (int i = 0; i < coversArr.length(); i++) {
                        coverIds.add(coversArr.optString(i));
                    }
                    book.setCoverIds(coverIds);
                }


                JSONArray authorsArr = jsonObject.optJSONArray("authors");
                if (authorsArr != null) {
                    List<String> authorIds = new ArrayList<>();
                    for (int i = 0; i < authorsArr.length(); i++) {
                        try {
                            String authorId = authorsArr.getJSONObject(i)
                                    .getJSONObject("author")
                                    .getString("key")
                                    .replace("/authors/", "");
                            authorIds.add(authorId);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    book.setAuthorIds(authorIds);
                    List<String> authorNameList = authorIds.stream()
                            .map(id -> authorRepository.findById(id))
                            .map(author -> {
                                if (author.isEmpty()) return "Unknown Author";
                                return author.get().getName();
                            }).collect(Collectors.toList());

                    book.setAuthorNames(authorNameList);
                }


                JSONObject createdObject = jsonObject.optJSONObject("created");
                LocalDate localDate = LocalDate.parse(createdObject.optString("value"), dateTimeFormatter);
                book.setPublishedDate(localDate);

                // Persist using Repository
                bookRepository.save(book);

                System.out.println("Name: "+book.getName());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void start() {
        initAuthors();
        initWorks();
    }

    @Bean
    public CqlSessionBuilderCustomizer sessionBuilderCustomizer (DataStaxAstraProperties astraProperties) {
        Path bundle = astraProperties.getSecureConnectBundle().toPath();
        return cqlSessionBuilder -> cqlSessionBuilder.withCloudSecureConnectBundle(bundle);
    }


}
