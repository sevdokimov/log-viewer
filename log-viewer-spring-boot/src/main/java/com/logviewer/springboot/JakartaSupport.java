package com.logviewer.springboot;

import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;

public class JakartaSupport {

    private JakartaSupport() {

    }

    public static boolean useJakarta(ClassLoader classLoader) {
        try {
            classLoader.loadClass("jakarta.servlet.http.HttpServlet");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Spring 3.0 and higher uses "jakarta.servlet.*", but before 3.0 it uses "javax.servlet.*". Log-viewer must be compatible
     * with both. This method loads and patches a class, it replaces "javax.servlet.*" with "jakarta.servlet.*" is needed.
     */
    public static Class loadJavaxOrJakartaClass(ClassLoader classLoader, String classname) {
        try {
            if (useJakarta(classLoader))
                return new JakartaClassLoader(classLoader, classname).loadClass(classname);

            return classLoader.loadClass(classname);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String javax2jakarta(String s) {
        if (s.startsWith("javax/servlet/") || s.startsWith("javax/websocket/"))
            return "jakarta" + s.substring("javax".length());

        return s.replace("Ljavax/websocket/", "Ljakarta/websocket/").replace("Ljavax/servlet/", "Ljakarta/servlet/");
    }

    private static class JakartaClassLoader extends ClassLoader {

        private final String patchedClassName;

        public JakartaClassLoader(ClassLoader classLoader, String patchedClassName) {
            super(classLoader);
            this.patchedClassName = patchedClassName;
        }

        private byte[] loadPatchedClass(String classname) {
            URL classUrl = getResource(classname.replace('.', '/') + ".class");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);

            try (DataInputStream in = new DataInputStream(classUrl.openStream())) {
                long magicMajorMinor = in.readLong(); // magic (0xCAFEBABE) + minor_version + major_version
                dataOut.writeLong(magicMajorMinor);

                int constCount = in.readUnsignedShort();
                dataOut.writeShort(constCount);

                byte[] b3 = new byte[3];

                for (int i = 0; i < constCount - 1; i++) {
                    byte constType = in.readByte();
                    dataOut.writeByte(constType);

                    switch (constType) {
                        case 7: // CONSTANT_Class
                        case 8: // CONSTANT_String_info
                        case 19: // CONSTANT_Module_info
                        case 20: // CONSTANT_Package
                        case 16: // CONSTANT_MethodType
                            int b2 = in.readUnsignedShort();
                            dataOut.writeShort(b2);
                            break;
                        case 9: // CONSTANT_Fieldref
                        case 10: // CONSTANT_Methodref
                        case 11: // CONSTANT_InterfaceMethodref
                        case 3: // CONSTANT_Integer
                        case 4: // CONSTANT_Float
                        case 12: // CONSTANT_NameAndType
                        case 17: // CONSTANT_Dynamic
                        case 18: // CONSTANT_InvokeDynamic
                            int b4 = in.readInt();
                            dataOut.writeInt(b4);
                            break;
                        case 5: // CONSTANT_Long
                        case 6: // CONSTANT_Double
                            long b8 = in.readLong();
                            dataOut.writeLong(b8);
                            break;
                        case 1: // CONSTANT_Utf8
                            String utf = in.readUTF();
                            utf = javax2jakarta(utf);
                            dataOut.writeUTF(utf);
                            break;
                        case 15: // CONSTANT_MethodHandle
                            in.readFully(b3);
                            dataOut.write(b3);
                            break;
                        default:
                            throw new RuntimeException("Unknown constant tag: " + constType);
                    }
                }

                StreamUtils.copy(in, dataOut);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return out.toByteArray();
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if ((name.equals(patchedClassName) || name.startsWith(patchedClassName + '$'))
                    && findLoadedClass(name) == null) {
                byte[] bytecode = loadPatchedClass(name);
                Class<?> res = defineClass(name, bytecode, 0, bytecode.length);
                if (resolve)
                    resolveClass(res);

                return res;
            }

            return super.loadClass(name, resolve);
        }
    }
}
