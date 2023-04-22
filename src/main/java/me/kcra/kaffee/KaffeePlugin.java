package me.kcra.kaffee;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import me.kcra.kaffee.util.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class KaffeePlugin implements Plugin {
    private final Map<String, String> translations = new HashMap<>() {
        {
            put("abstract", "abstrakt");
            put("boolean", "boolesch");
            put("break", "abbruch");
            put("case", "fall");
            put("catch", "fangen");
            put("char", "charakter");
            put("class", "klasse");
            put("const", "konst");
            put("continue", "fortfahren");
            put("default", "standard");
            put("do", "machen");
            put("double", "doppelt");
            put("else", "sonst");
            put("enum", "enum");
            put("extends", "erweitert");
            put("final", "final");
            put("finally", "endlich");
            put("float", "float");
            put("for", "für");
            put("if", "wenn");
            put("implements", "implementiert");
            put("import", "importieren");
            put("instanceof", "instanzvon");
            put("int", "integer");
            put("interface", "schnittstelle");
            put("long", "lang");
            put("native", "natürlich");
            put("new", "neu");
            put("package", "paket");
            put("private", "privat");
            put("protected", "geschützt");
            put("public", "öffentlich");
            put("return", "zurückgeben");
            put("short", "kurz");
            put("static", "statisch");
            put("strictfp", "strengfp");
            put("switch", "umschalten");
            put("synchronized", "synchronisiert");
            put("this", "dieses");
            put("throw", "werfen");
            put("throws", "wirft");
            put("transient", "flüchtig");
            put("try", "versuchen");
            put("void", "leer");
            put("volatile", "flüchtig");
            put("while", "während");
        }
    };

    static {
        UnsafeUtil.addOpens(KaffeePlugin.class);
    }

    @Override
    public String getName() {
        return "Kaffee";
    }

    @Override
    public void init(JavacTask javacTask, String... strings) {
        final Context context = ((BasicJavacTask) javacTask).getContext();
        final Unsafe unsafe = UnsafeUtil.getUnsafe();

        // stage one: rename enum names
        try {
            final long nameFieldOffset = unsafe.objectFieldOffset(Tokens.TokenKind.class.getDeclaredField("name"));

            for (final Tokens.TokenKind kind : Tokens.TokenKind.values()) {
                final String translation = translations.get(kind.name);
                if (translation != null) {
                    unsafe.putObject(kind, nameFieldOffset, translation);
                }
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // stage two: rename data-driven names
        final Names names = Names.instance(context);
        try {
            for (final Field nameField : Names.class.getDeclaredFields()) {
                if (nameField.getType() == Name.class) {
                    final String translation = translations.get(nameField.get(names).toString());
                    if (translation != null) {
                        unsafe.putObject(names, unsafe.objectFieldOffset(nameField), translation);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // stage three: rename token index, on newer JDKs, so ignore the exceptions
        final Tokens tokens = Tokens.instance(context);
        try {
            final Field keywordsField = Tokens.class.getDeclaredField("keywords");
            keywordsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            final Map<String, Tokens.TokenKind> keywords = (Map<String, Tokens.TokenKind>) keywordsField.get(tokens);

            for (final Map.Entry<String, String> entry : translations.entrySet()) {
                final Tokens.TokenKind kind = keywords.remove(entry.getKey());
                if (kind != null) {
                    keywords.put(entry.getValue(), kind);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }
}
