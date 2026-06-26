# Welcome to the Blog Api Project
This is a SpringBoot Project served as backend server for my personal blog https://www.contentfarm-info.com.
This project aim to practice DDD and Hexagon Architecture.

# Module Architecture
![module Architecture](/readme-file/module-architecture.png)

# Add New Service Using Hexagon Architecture Concept
Using relational Database as an example. There are 3 Steps.

# How To Run
```sh
cd docker-compose

# Full Backend
docker-compose up -d

# Infra only
docker compose up postgres elasticsearch localstack -d
```

## Step1: Define interface(Input or Output port) in domain layer
```java
// contentfarm-domain/src/main/java/com/contentfarm/outputport/blogpost/persistence/IBlogPostPersistenceCommandService.java
public interface IBlogPostPersistenceCommandService {
    BlogPostDomainModel upsertBlogPost(BlogPostDomainModel blogPostDomainModel);
    String deleteBlogPostById(String id);
    void addBlogPostTag(List<String> blogPostTagNameList);
    void deleteBlogPostTag(List<String> blogPostTagNameList);
}
```

## Step2: Add new module and Implement domain layer interface
Then create a new module named content-farm-relational-database and implement the domain layer interface.
```java
// contentfarm-relational-database/src/main/java/com/contentfarm/persistence/blogpost/service/BlogPostPersistenceCommandService.java
...
@Service
@RequiredArgsConstructor
public class BlogPostPersistenceCommandService implements IBlogPostPersistenceCommandService {
    private final IBlogPostDao blogPostDao;
    private final IBlogPostTagDao blogPostTagDao;
    private final BlogPostEntityMapper blogPostEntityMapper;

    @Override
    public BlogPostDomainModel upsertBlogPost(BlogPostDomainModel blogPostDomainModel) {
        var blogPostEntity = blogPostEntityMapper.mapToBlogPostEntity(blogPostDomainModel);
        if (null == blogPostEntity.getId()) {
            String uuid = UUID.randomUUID().toString();
            blogPostEntity.setId(uuid);
            blogPostDomainModel.setId(uuid);
        } else {
            blogPostEntity.setLastUpdatedDateTime(ContentFarmLocaleDateTimeUtils.ofNow());
        }
        blogPostDao.save(blogPostEntity);
        return blogPostDomainModel;
    }
    ...
}
```

## Step2.1: Test the implementation within new module
As the new module is relational database, the test can be set up by Testcontainer easily.
```java
// contentfarm-relational-database/src/test/java/com/contentfarm/persistence/blogpost/TestContainerRelationalDatabase.java
...
@Testcontainers
public class TestContainerRelationalDatabase {
    @ServiceConnection
    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:17.4")
            .withDatabaseName("testing")
            .withUsername("postgres")
            .withPassword("postgres")
            .withExposedPorts(5432)
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(5433), new ExposedPort(5432)))
            ));
}
```

```java
// contentfarm-relational-database/src/test/java/com/contentfarm/persistence/blogpost/service/BlogPostPersistenceCommandServiceTest.java
...
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ImportTestcontainers(TestContainerRelationalDatabase.class)
@SpringBootTest(classes = TestConfiguration.class)
@Transactional
class BlogPostPersistenceCommandServiceTest {
    @Autowired
    BlogPostPersistenceCommandService blogPostPersistenceCommandService;

    @Test
    void upsertBlogPost() {
        long count = blogPostDao.count();
        Assertions.assertEquals(0, count);

        BlogPostDomainModel blogPostDomainModel = BlogPostDomainModel
                .builder()
                .id(null)
                .title("testingTitle1")
                .summary("testingSummary1")
                .authorId("testingAuthorId")
                .tagList(List.of("Java", "Testing"))
                .coverImageUrl("image.png")
                .contentType(BlogPostContentType.MARKDOWN)
                .contentFileName("content.md")
                .createdDateTime(LocalDateTime.of(2025, 3, 20, 0, 0, 0))
                .build();
        var savedModel = blogPostPersistenceCommandService.upsertBlogPost(blogPostDomainModel);
        String savedBlogPostId = savedModel.getId();
        var blogPostEntity = blogPostDao.getReferenceById(savedModel.getId());
        Assertions.assertNotNull(blogPostEntity);
        Assertions.assertEquals(savedBlogPostId, blogPostEntity.getId());
        Assertions.assertEquals("testingTitle1", blogPostEntity.getTitle());
        Assertions.assertEquals("testingAuthorId", blogPostEntity.getAuthorId());
        Assertions.assertEquals(BlogPostContentType.MARKDOWN, blogPostEntity.getContentType());
        Assertions.assertEquals("content.md", blogPostEntity.getContentFileName());
        Assertions.assertEquals(LocalDateTime.of(2025, 3, 20, 0, 0, 0), blogPostEntity.getCreatedDateTime());
        ...
    }
    ...
}
```

## Step3: Inject the service by interface in domain layer
Lastly the interface in Step1 can be injected in domain layer, e.g. domain service.
```java
// contentfarm-domain/src/main/java/com/contentfarm/domainservice/blogpost/BlogPostDomainService.java
...
@Service
@RequiredArgsConstructor
public class BlogPostDomainService implements IBlogPostWebDomainService, IAdminBlogPostCommandService {
    private final IBlogPostPersistenceCommandService blogPostPersistenceCommandService;
    ...
}
```

## Step3.1: Test the new service(port) in domain layer by mocking
We can create mock or stub by implement the interface(in Step 1) in domain layer test.
```java
// contentfarm-domain/src/test/java/com/contentfarm/domainservice/blogpost/BlogPostDomainServiceTest.java
...
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlogPostDomainServiceTest {
    private final BlogPostDomainService blogPostDomainService =
            new BlogPostDomainService(
                    new IBlogPostPersistenceQueryServiceStub(),
                    new IBlogPostPersistenceCommandServiceStub(),
                    new IBlogPostSearchServiceStub(),
                    new IBlogPostIndexServiceStub(),
                    new IBlogPostFileQueryServiceStub()
            );
    ...

    private class IBlogPostPersistenceCommandServiceStub implements IBlogPostPersistenceCommandService {

        @Override
        public BlogPostDomainModel upsertBlogPost(BlogPostDomainModel blogPostDomainModel) {
            return null;
        }

        @Override
        public String deleteBlogPostById(String id) {
            return "";
        }

        @Override
        public void addBlogPostTag(List<String> blogPostTagNameList) {

        }
        ...
    }
}
```
