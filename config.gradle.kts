import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

// 定义一个函数来获取当前日期的版本号
fun getProjectVersion(): String {
    val df = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
    return df.format(Calendar.getInstance().time)
}

var versions = mapOf(
    "guava" to "com.google.guava:guava:31.0.1-jre",
    "hutool-all" to "cn.hutool:hutool-all:5.8.5",
    "javax-servlet" to "javax.servlet:javax.servlet-api:4.0.1",
    "apache-dubbo" to "org.apache.dubbo:dubbo:2.7.0",
    "spring-web" to "org.springframework:spring-web:5.2.4.RELEASE",
    "slf4j" to "org.slf4j:slf4j-api:1.7.36",
    "lombok" to "org.projectlombok:lombok:1.18.34",
    "fastjson" to "com.alibaba:fastjson:1.2.79",
    "javassist" to "javassist:javassist:3.12.1.GA",
    "byte-buddy" to "net.bytebuddy:byte-buddy:1.12.6",
    "byte-buddy-agent" to "net.bytebuddy:byte-buddy-agent:1.12.6",
    "junit" to "junit:junit:4.12",
    "junit.jupiter.api" to "org.junit.jupiter:junit-jupiter-api:5.8.2",
    "junit.jupiter.engine" to "org.junit.jupiter:junit-jupiter-engine:5.8.2"
)
extra["versions"] = versions

val currentVersion = getProjectVersion()
println("Project Version: $currentVersion")
extra["projectVersion"] = currentVersion

