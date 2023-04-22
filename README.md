# kaffee

Do you ever feel tired of writing English words in your Java code?
Do you wish it was a little bit more German?
Fear no more, we've got you covered.

Tested with Temurin 17.0.1, newer versions should work as well.

## Usage

Apply the plugin to `javac` with `javac -cp kaffee-0.0.1-SNAPSHOT.jar -Xplugin:Kaffee <your compilation target>`.

Available translations are placed in the [`KaffeePlugin.java`](https://github.com/zlataovce/kaffee/tree/master/src/main/java/me/kcra/kaffee/KaffeePlugin.java#L20) file.

```
öffentlich klasse Example {
    öffentlich statisch leer main(String[] args) {
        für (integer i = 1; i <= 5; i++) {
            umschalten (i) {
                fall 1:
                    System.out.println("Hallo, Welt!");
                    abbruch;
                fall 5:
                    System.out.println("Auf Wiedersehen, Welt!");
                    abbruch;
                standard:
                    System.out.println("Hallo, Welt noch einmal!");
                    abbruch;
            }
        }
    }
}
```

## Licensing

This project is licensed under the [MIT License](https://github.com/zlataovce/kaffee/tree/master/LICENSE).
