# CyberGear TaskManager

*Version:* ***0.0.3***

 - cleaned manifest from not necessary tags

*Version:* ***0.0.2***

 - must be better callback handling

*Version:* ***0.0.1***, *Min SDK version:* ***10*** *(* *Android:* ***2.3.3*** *)*

---

## Description

This tool will help you to handle async tasks between recreating Activities on rotation. It relinks calbacks back to activity, without any truble

## Including to your project

Project build.gradle
```Groovy
allprojects {
    repositories {
        ...
        maven {
            url "https://github.com/CyberGear/TaskManager/raw/master/repo/"
        }
    }
}
```

module build.gradle
```Groovy
dependencies {
    ...
    compile 'lt.cybergear:taskmanager:0.0.3'
}
```

## [Usage Example](https://github.com/CyberGear/TaskManager/blob/master/app/src/main/java/lt/cybergear/taskmanager/test/MainActivity.java)
