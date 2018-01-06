<a href="https://promisesaplus.com/"><img src="https://promisesaplus.com/assets/logo-small.png" align="right" /></a>

# Promise

A promise represents a future value (usually of an asynchronous operation).

This is a simple implementation of Promises/A+ in Java, based on RxJava's architecture.

## Installation

#### **Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```groovy
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

#### **Step 2.** Add the dependency

```groovy
dependencies {
  compile 'com.github.SMontiel:Promise:0.2.1'
}
```
## Usage

```java
Promise<String> p = Promise.resolve("Hello world");
p.then(new Consumer<String>() {
        @Override
        public void accept(String s) {
            System.out.println(s);
        }
    }).then(new Function<String, Integer>() {
        @Override
        public Integer apply(String s) {
            return s.length();
        }
    }).then(new Consumer<Integer>() {
        @Override
        public void accept(Integer integer) {
            System.out.println("Length of " + integer);
        }
    }).done();
```

Output:

```bash
Hello world!
Length of 12
```

## Building

Building `Promise` with Gradle is fairly straight forward:

```bash
git clone https://github.com/SMontiel/Promise.git
cd Promise
./gradlew build
```
## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/SMontiel/Promise/issues).

## Acknowledgements