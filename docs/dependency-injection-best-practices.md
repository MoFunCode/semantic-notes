# Dependency Injection Best Practices in Spring Boot

## Table of Contents
- [The Three Ways of Dependency Injection](#the-three-ways-of-dependency-injection)
- [Why Constructor Injection is Best](#why-constructor-injection-is-best)
- [Using Lombok for Clean Code](#using-lombok-for-clean-code)
- [Real Examples from This Project](#real-examples-from-this-project)
- [Testing with Dependency Injection](#testing-with-dependency-injection)
- [Common Mistakes to Avoid](#common-mistakes-to-avoid)

---

## The Three Ways of Dependency Injection

### 1. Constructor Injection (✅ RECOMMENDED)

```java
@RestController
public class OpenAIController {

    private final OpenAIService openAIService;  // final = immutable

    // Constructor injection - Spring automatically calls this
    public OpenAIController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }
}
```

**With Lombok (even cleaner):**

```java
@RestController
@RequiredArgsConstructor  // Lombok generates the constructor
public class OpenAIController {

    private final OpenAIService openAIService;

    // No constructor needed - Lombok generates it!
}
```

### 2. Field Injection (❌ AVOID)

```java
@RestController
public class OpenAIController {

    @Autowired  // Don't do this!
    private OpenAIService openAIService;
}
```

**Why to avoid:**
- Not immutable (can be changed)
- Hard to test (requires Spring context)
- Can be null at runtime
- Hides dependencies

### 3. Setter Injection (⚠️ Rarely needed)

```java
@RestController
public class OpenAIController {

    private OpenAIService openAIService;

    @Autowired
    public void setOpenAIService(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }
}
```

**When to use:**
- Optional dependencies
- Circular dependencies (though this is a code smell)
- Legacy code migration

---

## Why Constructor Injection is Best

### 1. Immutability ✅

```java
private final OpenAIService openAIService;  // Can't be changed after construction
```

**Benefits:**
- Thread-safe by default
- Prevents accidental reassignment
- Makes code more predictable

### 2. Required Dependencies are Explicit ✅

```java
public OpenAIController(OpenAIService openAIService) {
    this.openAIService = openAIService;  // Must be provided!
}
```

**Benefits:**
- Can't create the object without dependencies
- Constructor signature documents what's needed
- No `NullPointerException` at runtime
- Fail fast during application startup

### 3. Testability ✅

```java
@Test
void testListModels() {
    // Easy to create without Spring!
    OpenAIService mockService = mock(OpenAIService.class);
    OpenAIController controller = new OpenAIController(mockService);

    // Test the controller
    when(mockService.listModels()).thenReturn(someData);
    Object result = controller.listModels();
    assertNotNull(result);
}
```

**Benefits:**
- Create instances without Spring container
- Easy to pass mocks or test doubles
- Faster tests (no Spring context needed)

### 4. No Annotations Required ✅

```java
// No @Autowired needed - Spring is smart enough!
public OpenAIController(OpenAIService openAIService) {
    this.openAIService = openAIService;
}
```

**Since Spring 4.3:**
- `@Autowired` is optional on single constructors
- Less code, cleaner syntax
- Less coupling to Spring framework

---

## Using Lombok for Clean Code

### The `@RequiredArgsConstructor` Annotation

**What you write:**

```java
@Service
@RequiredArgsConstructor
public class OpenAIService {
    private final OpenAIClient openAIClient;
    private final MetricsService metricsService;
}
```

**What Lombok generates at compile time:**

```java
@Service
public class OpenAIService {
    private final OpenAIClient openAIClient;
    private final MetricsService metricsService;

    public OpenAIService(OpenAIClient openAIClient,
                        MetricsService metricsService) {
        this.openAIClient = openAIClient;
        this.metricsService = metricsService;
    }
}
```

### Multiple Dependencies Example

```java
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NotesController {

    private final NotesService notesService;
    private final OpenAIService openAIService;
    private final MetricsService metricsService;
    private final UserService userService;

    // All four are injected via constructor automatically!

    @GetMapping("/search")
    public List<Note> search(@RequestParam String query) {
        metricsService.recordSearch();
        return notesService.semanticSearch(query);
    }
}
```

---

## Real Examples from This Project

### Example 1: OpenAIService

**File:** `src/main/java/com/github/mofuncode/semantic_notes/service/OpenAIService.java`

```java
@Slf4j
@Service
public class OpenAIService {

    private final String apiKey;
    private OpenAIClient openAIClient;

    // ✅ Constructor injection for configuration value
    public OpenAIService(@Value("${openai.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    // ⚠️ Late initialization - sometimes necessary
    @PostConstruct
    private void initializeClient() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
```

**Why this pattern:**
- `apiKey` is injected via constructor ✅
- `openAIClient` needs complex initialization
- Uses `@PostConstruct` for post-construction setup

### Example 2: Better Approach with Configuration Class

```java
@Configuration
public class OpenAIConfig {

    @Bean
    public OpenAIClient openAIClient(@Value("${openai.api.key}") String apiKey) {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}

@Service
@RequiredArgsConstructor  // Lombok
public class OpenAIService {

    private final OpenAIClient openAIClient;  // ✅ Now it's final!

    public Object listModels() {
        return openAIClient.models().list();
    }
}
```

**Benefits:**
- Separates configuration from business logic
- `OpenAIClient` is now a reusable Spring bean
- `OpenAIService` is fully immutable
- Easier to test (mock `OpenAIClient`)

### Example 3: Controller Pattern

```java
@RestController
@RequestMapping("/api/openai")
@RequiredArgsConstructor
public class OpenAIController {

    private final OpenAIService openAIService;  // final = immutable

    @GetMapping("/models")
    public Object listModels() {
        return openAIService.listModels();
    }

    @GetMapping("/models/{modelId}")
    public Object getModel(@PathVariable String modelId) {
        return openAIService.getModel(modelId);
    }
}
```

---

## Testing with Dependency Injection

### Unit Test Example (No Spring)

```java
class OpenAIControllerTest {

    private OpenAIService mockService;
    private OpenAIController controller;

    @BeforeEach
    void setUp() {
        mockService = mock(OpenAIService.class);
        controller = new OpenAIController(mockService);  // Easy!
    }

    @Test
    void testListModels() {
        // Arrange
        Object expectedModels = new Object();
        when(mockService.listModels()).thenReturn(expectedModels);

        // Act
        Object result = controller.listModels();

        // Assert
        assertSame(expectedModels, result);
        verify(mockService).listModels();
    }
}
```

### Integration Test Example (With Spring)

```java
@SpringBootTest
@AutoConfigureMockMvc
class OpenAIControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean  // Spring replaces the real bean with a mock
    private OpenAIService openAIService;

    @Test
    void testListModelsEndpoint() throws Exception {
        when(openAIService.listModels()).thenReturn(someData);

        mockMvc.perform(get("/api/openai/models"))
               .andExpect(status().isOk())
               .andExpect(content().json(expectedJson));
    }
}
```

---

## Common Mistakes to Avoid

### ❌ Mistake 1: Field Injection

```java
// DON'T DO THIS
@RestController
public class MyController {
    @Autowired
    private MyService myService;  // Not final, hard to test
}
```

**Fix:**
```java
// DO THIS
@RestController
@RequiredArgsConstructor
public class MyController {
    private final MyService myService;  // Immutable, testable
}
```

### ❌ Mistake 2: Not Using `final`

```java
// DON'T DO THIS
@RequiredArgsConstructor
public class MyService {
    private OpenAIClient client;  // Not final - Lombok won't inject it!
}
```

**Fix:**
```java
// DO THIS
@RequiredArgsConstructor
public class MyService {
    private final OpenAIClient client;  // final = injected by Lombok
}
```

### ❌ Mistake 3: Circular Dependencies

```java
// DON'T DO THIS
@Service
public class ServiceA {
    private final ServiceB serviceB;  // A depends on B
}

@Service
public class ServiceB {
    private final ServiceA serviceA;  // B depends on A - CIRCULAR!
}
```

**Fix:** Refactor your design. Consider:
- Extract common logic to a third service
- Use events/messaging instead of direct calls
- Rethink your architecture

### ❌ Mistake 4: Too Many Dependencies

```java
// CODE SMELL - Too many dependencies!
@RequiredArgsConstructor
public class MyController {
    private final ServiceA serviceA;
    private final ServiceB serviceB;
    private final ServiceC serviceC;
    private final ServiceD serviceD;
    private final ServiceE serviceE;
    private final ServiceF serviceF;  // 6+ dependencies = problem!
}
```

**Fix:**
- Violates Single Responsibility Principle
- Create a facade service that combines related services
- Split the controller into multiple controllers

---

## When You DON'T Need `@Autowired`

**Spring 4.3+:** If a class has exactly ONE constructor, `@Autowired` is optional.

```java
// Both work identically:

// Option 1 - No @Autowired (cleaner, recommended)
public OpenAIController(OpenAIService openAIService) {
    this.openAIService = openAIService;
}

// Option 2 - Explicit @Autowired (unnecessary but not wrong)
@Autowired
public OpenAIController(OpenAIService openAIService) {
    this.openAIService = openAIService;
}
```

**When you DO need `@Autowired`:**
- Multiple constructors (mark which one Spring should use)
- Setter injection
- Field injection (but don't use field injection!)

---

## The Golden Rules

1. **Always use Constructor Injection** - Never field injection
2. **Make dependencies `final`** - Immutability prevents bugs
3. **Use `@RequiredArgsConstructor`** - Let Lombok eliminate boilerplate
4. **Skip `@Autowired` on constructors** - It's optional in modern Spring
5. **Separate configuration from business logic** - Use `@Configuration` classes
6. **Keep dependencies minimal** - If you have 5+ dependencies, refactor
7. **Test without Spring when possible** - If you can `new MyClass(mock)`, you're doing it right

---

## Quick Reference

| Pattern | Use Case | Example |
|---------|----------|---------|
| `@RequiredArgsConstructor` + `final` fields | Controllers, Services | `@RequiredArgsConstructor public class MyController { private final MyService service; }` |
| `@Configuration` + `@Bean` | Creating complex beans | `@Bean public OpenAIClient client() { ... }` |
| `@Value` in constructor | Injecting config values | `public MyService(@Value("${api.key}") String key)` |
| Manual constructor | When you need validation logic | `public MyService(Dep dep) { validate(dep); this.dep = dep; }` |

---

## Additional Resources

- [Spring Framework Documentation - Dependency Injection](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-constructor-injection)
- [Project Lombok - @RequiredArgsConstructor](https://projectlombok.org/features/constructor)
- [Baeldung - Constructor Injection in Spring](https://www.baeldung.com/constructor-injection-in-spring)

---

**Last Updated:** December 2025
**Project:** Semantic Notes
**Author:** Development Team
