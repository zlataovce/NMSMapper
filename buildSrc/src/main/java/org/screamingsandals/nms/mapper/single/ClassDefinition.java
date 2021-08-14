package org.screamingsandals.nms.mapper.single;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Data;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

@Data
public class ClassDefinition {
    private int modifier;
    private Type type = Type.CLASS;
    private Link superclass = null;
    private final ObjectList<Link> interfaces = new ObjectArrayList<>();
    private final Object2ObjectOpenHashMap<MappingType, String> mapping = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, FieldDefinition> fields = new Object2ObjectOpenHashMap<>();
    private final ObjectList<MethodDefinition> methods = new ObjectArrayList<>();
    private final ObjectList<ConstructorDefinition> constructors = new ObjectArrayList<>();

    private transient String joinedKey;
    private transient String pathKey;

    @Data
    public static class FieldDefinition {
        private int modifier;
        private final Link type;
        private final Object2ObjectOpenHashMap<MappingType, String> mapping = new Object2ObjectOpenHashMap<>();
    }

    @Data
    public static class MethodDefinition {
        private int modifier;
        private final Link returnType;
        private final Object2ObjectOpenHashMap<MappingType, String> mapping = new Object2ObjectOpenHashMap<>();
        private final ObjectList<Link> parameters = new ObjectArrayList<>();
    }

    @Data
    public static class ConstructorDefinition {
        private int modifier;
        private final ObjectList<Link> parameters = new ObjectArrayList<>();
    }

    @Data
    public static class Link {
        private final String type;
        private final boolean nms;

        public static Link casualLink(String type) {
            return new Link(type, false);
        }

        public static Link nmsLink(String type) {
            return new Link(type, true);
        }

        public String joined() {
            return (nms ? "&" : "") + type;
        }
    }

    // TODO: figure out how to do custom serializer (configurate is refusing to do anything for some reason)
    public ConfigurationNode asNode(ConfigurationNode node) throws SerializationException {
        node.node("mapping").set(mapping);
        node.node("modifier").set(modifier);
        node.node("superclass").set(superclass != null ? superclass.joined() : null);
        var interfacesN = node.node("interfaces");
        interfaces.forEach(link -> {
            try {
                interfacesN.appendListNode().set(link.joined());
            } catch (SerializationException serializationException) {
                serializationException.printStackTrace();
            }
        });
        var fieldsN = node.node("fields");
        fields.forEach((k,v) -> {
            try {
                var f = fieldsN.appendListNode();
                f.node("type").set(v.getType().joined());
                f.node("mapping").set(v.getMapping());
                f.node("modifier").set(v.getModifier());
            } catch (SerializationException serializationException) {
                serializationException.printStackTrace();
            }
        });
        var constructorsN = node.node("constructors");
        constructors.forEach(v -> {
            try {
                var constructorN = constructorsN.appendListNode();
                constructorN.node("modifier").set(v.getModifier());
                v.getParameters().forEach(link -> {
                    try {
                        var listNode = constructorN.node("parameters").appendListNode();
                        listNode.set(link.joined());
                    } catch (SerializationException serializationException) {
                        serializationException.printStackTrace();
                    }
                });
            } catch (SerializationException e) {
                e.printStackTrace();
            }
        });
        var methodsN = node.node("methods");
        methods.forEach(v -> {
            try {
                var methodN = methodsN.appendListNode();
                methodN.node("returnType").set(v.getReturnType().joined());
                methodN.node("mapping").set(v.getMapping());
                methodN.node("modifier").set(v.getModifier());
                v.getParameters().forEach(link -> {
                    try {
                        var listNode = methodN.node("parameters").appendListNode();
                        listNode.set(link.joined());
                    } catch (SerializationException serializationException) {
                        serializationException.printStackTrace();
                    }
                });
            } catch (SerializationException serializationException) {
                serializationException.printStackTrace();
            }
        });
        return node;
    }

    public enum Type {
        CLASS, // including records etc.
        INTERFACE,
        ENUM,
        ANNOTATION
    }
}
