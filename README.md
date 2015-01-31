# CyberGear TaskManager
*Version:* ***0.0.1***, *Min SDK version:* ***2.3.3***

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
    compile 'lt.cybergear:taskmanager:0.0.1'
}
```

## Usage

There is example in 'app' module 'MainActivity' class

