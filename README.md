<a href="https://promisesaplus.com/"><img src="https://promisesaplus.com/assets/logo-small.png" align="right" /></a>

# Promise

A promise represents a future value (usually of an asynchronous operation).

This is a simple implementation of Promises/A+ in Java, based on RxJava's architecture.

## Installation

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

## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](issues).

## Acknowledgements