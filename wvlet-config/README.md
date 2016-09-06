wvlet-config
===

## Application configuration flow

- User specifies an environment (e.g., `test`, `staging`, `production`, etc)
- Read a configuration file (YAML)
  - wvlet-config will search `configpath(s)` to find the YAML file  
  - If the target YAML file is not found, it uses the default configuration
  - When YAML file is found, it searches for configuration for the target environment.
       - If no configuration for the target environment is found, uses `default` environment configuration. 
       - If `default` environment is also not found, it uses the provided default object

- Supply additional configurations (e.g., confidential information such as password, apikey, etc.)
  - Read these configurations in a secure manner and create a `Properties` object.
  - Override your configurations with this `Properties` object.

- Use ConfigBuider to build configurations


## Usage

**config/access-log.yml**:
```
default:
  file: log/access.log
  max_files: 50
```

**config/db-log.yml**:
```
default:
  file: log/db.log
```

**config/server.yml**
```
default:
  host: localhost
  port: 8080

# Override port number for development
development:
  <<: *default
  port: 9000
```

**config.properties**:
```
# [prefix](@[tag])?.[param]=(value)
log@access.file=/path/to/access.log
log@db.file=/path/to/db.log
log@db.log.max_files=250
server.host=mydomain.com


server.password=xxxxxyyyyyy
```

code:
```scala
import wvlet.config.Config
import wvlet.obj.tag.@@

// Configulation classes can have default values
// Configuration class name convention: xxxxConfig (xxxx will be the prefix)
case class LogConfig(file:String, maxFiles:Int=100, maxSize:Int=10485760)
case class ServerConfig(host:String, port:Int, password:String)

// To use the same configuration class for different purposes, use type tag (@@ Tag)
trait Access
trait Db

val config = 
  Config(env="development", configPaths="./config")
    .registerFromYaml[LogConfig @@ Access]("access-log.yml")
    .registerFromYaml[LogConfig @@ Db]("db-log.yml")
    .registerFromYaml[ServerConfig]("server.yml")
    .overrideWithPropertiesFile("config.properties")
    
val accessLogConfig = config.of[LogConfig @@ Access]
// LogConfig("/path/to/access.log",50,104857600)

val dbLogConfig = config.of[LogConfig @@ Db]
// LogConfig("/path/to/db.log",250,104857600)

val serverConfig = config.of[ServerConfig]
// ServerConfig(host="mydomain.com",port=9000,password="xxxxxyyyyyy")

```

### Show configuration changes

To see the effective configurations, use `Config.getConfigChanges` method:
```scala
import wvlet.config.Config

val config =
  Config(env="development", configPaths="./config")

for(change <- config.getConfigChanges) {
  println(s"[${change.key}] default:${change.default}, current:${change.current}")
}
```
