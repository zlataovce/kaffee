package me.kcra.kaffee.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public final class UnsafeUtil {
    private static final Unsafe UNSAFE;
    private static final MethodHandle CLASS_MODULE;
    private static final MethodHandle CLASS_LOADER_MODULE;
    private static final MethodHandle METHOD_MODIFIERS;

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
            CLASS_MODULE = implLookup.findVirtual(Class.class, "getModule", moduleType);
            CLASS_LOADER_MODULE = implLookup.findVirtual(ClassLoader.class, "getUnnamedModule", moduleType);
            METHOD_MODIFIERS = implLookup.findSetter(Method.class, "modifiers", Integer.TYPE);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private UnsafeUtil() {
    }

    public static Unsafe getUnsafe() {
        return UNSAFE;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    public static void addOpens(Class<?> classBase) {
        try {
            final Method implAddOpensMethod = Module.class.getDeclaredMethod("implAddOpens", String.class);
            METHOD_MODIFIERS.invokeExact(implAddOpensMethod, Modifier.PUBLIC);

            final Set<Module> modules = new HashSet<>();

            final Module base = (Module) CLASS_MODULE.invokeExact(classBase);
            if (base.getLayer() != null) {
                modules.addAll(base.getLayer().modules());
            }
            modules.addAll(ModuleLayer.boot().modules());
            for (ClassLoader cl = classBase.getClassLoader(); cl != null; cl = cl.getParent()) {
                modules.add((Module) CLASS_LOADER_MODULE.invokeExact(cl));
            }

            for (final Module module : modules) {
                for (final String name : module.getPackages()) {
                    implAddOpensMethod.invoke(module, name);
                }
            }
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }
}
