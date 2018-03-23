# Vert.x.Dao
SimpleDao for Vert.x project

### Define entity class

```java
    @Table(name = "user_t")
    public class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;
    
        @Column(name = "other_name")
        private String userName;
    
        private Integer age;

        @Transient
        private Integer others;

        getter and setter...
    }
```
### CURD

```java
    Dao.create(vertx, jdbcConfig, dao ->

        User user = new User();
        user.setId(1);

        dao.selectOne(user, s -> {
            if (s == null) {
                handler.handle("id " + user.getId() + " does not exists.");
            } else {
                s.setName("maki");
                dao.updateSelective(s, z -> {});
            }
        })
    );
```

### CURD with transaction support

```java
    Dao.createTransactional(vertx, jdbcConfig, dao ->

        User user = new User();
        user.setName("maki");

        dao.insertSelective(user, z -> {
            user.setAge(12);
            dao.updateSelective(user, u -> {
                dao.commitAndClose();
            });
        })
    );
```

### Using (mvn package)

```xml
    <dependency>
        <groupId>com.mekki.vertx.dao</groupId>
        <artifactId>Vert.x.Dao</artifactId>
        <version>1.0.1</version>
        <scope>system</scope>
        <systemPath>${project.basedir}/libs/Vert.x.Dao-1.0.1.jar</systemPath>
    </dependency>
```