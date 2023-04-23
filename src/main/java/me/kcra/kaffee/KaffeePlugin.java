package me.kcra.kaffee;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KaffeePlugin implements Plugin {
    private static final Unsafe UNSAFE;

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
            put("extends", "erweitert");
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
            put("transient", "vorübergehend");
            put("try", "versuchen");
            put("void", "leer");
            put("volatile", "flüchtig");
            put("while", "während");
        }
    };

    static {
        try {
            final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);

            final Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            MethodHandles.publicLookup();
            final MethodHandles.Lookup implLookup = (MethodHandles.Lookup) UNSAFE.getObject(
                    UNSAFE.staticFieldBase(implLookupField),
                    UNSAFE.staticFieldOffset(implLookupField)
            );

            final MethodType moduleType = MethodType.methodType(Module.class);
            final MethodHandle classModule = implLookup.findVirtual(Class.class, "getModule", moduleType);
            final MethodHandle classLoaderModule = implLookup.findVirtual(ClassLoader.class, "getUnnamedModule", moduleType);
            final MethodHandle methodModifiers = implLookup.findSetter(Method.class, "modifiers", Integer.TYPE);

            final Method implAddOpensMethod = Module.class.getDeclaredMethod("implAddOpens", String.class);
            methodModifiers.invokeExact(implAddOpensMethod, Modifier.PUBLIC);

            final Set<Module> modules = new HashSet<>();

            final Module base = (Module) classModule.invokeExact(KaffeePlugin.class);
            if (base.getLayer() != null) {
                modules.addAll(base.getLayer().modules());
            }
            modules.addAll(ModuleLayer.boot().modules());
            for (ClassLoader cl = KaffeePlugin.class.getClassLoader(); cl != null; cl = cl.getParent()) {
                modules.add((Module) classLoaderModule.invokeExact(cl));
            }

            for (final Module module : modules) {
                for (final String name : module.getPackages()) {
                    implAddOpensMethod.invoke(module, name);
                }
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    @Override
    public String getName() {
        return "Kaffee";
    }

    @Override
    public void init(JavacTask javacTask, String... strings) {
        final Context context = ((BasicJavacTask) javacTask).getContext();

        // stage one: rename enum names
        try {
            final long nameFieldOffset = UNSAFE.objectFieldOffset(Tokens.TokenKind.class.getDeclaredField("name"));

            for (final Tokens.TokenKind kind : Tokens.TokenKind.values()) {
                final String translation = translations.get(kind.name);
                if (translation != null) {
                    UNSAFE.putObject(kind, nameFieldOffset, translation);
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
                        UNSAFE.putObject(names, UNSAFE.objectFieldOffset(nameField), translation);
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
