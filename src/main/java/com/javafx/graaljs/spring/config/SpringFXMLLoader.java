/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.javafx.graaljs.spring.config;

import com.sun.javafx.fxml.LoadListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.script.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Will load the FXML hierarchy as specified in the load method and register
 * Spring as the FXML Controller Factory. Allows Spring and Java FX to coexist
 * once the Spring Application context has been bootstrapped.
 */
@Component
public class SpringFXMLLoader {

    private final Logger LOG = LoggerFactory.getLogger(SpringFXMLLoader.class);

    //private static final String VARIABLE_SCOPE = "@(\"%s\")";

    public static Function<String,String> addQuotes = s -> "\"" + s + "\"";

    private final ResourceBundle resourceBundle;
    private final ApplicationContext context;
    private final ScriptEngine eng;
    private static CompiledScript script;
    private static String jsScript;
    private Context jsContext;
    private static Map<String, Object> FX_OBJECTS;


    private static final String SCRIPT_ATTRIBUTE = "script";

    private static final String SPACE = "\\s+";

    @Autowired
    public SpringFXMLLoader(ApplicationContext context, ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.context = context;
        this.eng = new ScriptEngineManager().getEngineByName("nashorn");
        this.jsContext = Context.newBuilder("js")
                                .option("js.nashorn-compat", "true")
                                .build();
        this.FX_OBJECTS = new HashMap<>();
    }
    

    public Parent load(String fxmlPath) {
        Parent parent = null;
        try {
            FXMLLoader loader = new FXMLLoader();
            Bindings bindings = new SimpleBindings();
            Value jsBindings = jsContext.getBindings("js");
            loader.setControllerFactory(context::getBean); //Spring now FXML Controller Factory
            loader.setResources(resourceBundle);
            loader.impl_setLoadListener(new FXMLLoaderListener(jsContext, jsBindings, eng, bindings));
            loader.setLocation(getClass().getResource(fxmlPath));
            parent = loader.load();
            /*** Using nashorn engine. Also can be used as Graaljs***/
//            bindings.put("fxObjects", globalVars)
//            script.eval(bindings);
            /*** Using GraalJS engine ***/
            jsBindings.putMember("FX_OBJECTS", FX_OBJECTS);
            jsContext.eval("js", jsScript);
        } catch (Exception ex) {
            LOG.error("Script exception {}", ex);
        }
        return parent;
    }


    private static class FXMLLoaderListener implements LoadListener {

        private final Logger LOG = LoggerFactory.getLogger(FXMLLoaderListener.class);

        /** GraalJS bindings ***/
        private Value jsBindings;
        /** Script engine instance ***/
        private ScriptEngine eng;
        /** Nashorn bindings ***/
        private Bindings bindings;

        public FXMLLoaderListener(Context context, Value value, ScriptEngine eng, Bindings bindings) {
            LOG.info("The engine is {}", context.getEngine().getImplementationName());
            jsBindings  = value;
            this.eng = eng;
            this.bindings = bindings;
        }


        @Override
        public void endElement(Object value) {
            LOG.info("FXML loader load element {}", value.getClass().getName());
            if(value instanceof Node) {
                Node node = (Node) value;
                if(node.getId() != null) {
                    /** Saving created nodes by fxmlloader which will be used during eval of the script by Graal or Nashorn ***/
                    LOG.info("Was found fx node with id {}", node.getId());
                    String idScope = String.format("#%s", node.getId()); //String.format(VARIABLE_SCOPE, (String.format("#%s", node.getId())));
                    bindings.put(idScope, value);
                    //jsBindings.putMember(idScope, value);
                    FX_OBJECTS.put(idScope, value);
                }
            }
        }


        @Override
        public void readImportProcessingInstruction(String target) {
            LOG.info("Reading instruction with value {}", target);
        }

        @Override
        public void readLanguageProcessingInstruction(String language) {

        }

        @Override
        public void readComment(String comment) {
            LOG.info("Reading comment with value {}", comment);
            String[] resourceMetadata = comment.split(SPACE);
            if(resourceMetadata[0].equals(SCRIPT_ATTRIBUTE)) {
                LOG.info("Found script with url {}", resourceMetadata[1]);
                jsScript = getResultAsString(getClass().getResourceAsStream(String.format("/%s", resourceMetadata[1])));
//                try {
//                    script = ((Compilable) eng).compile(jsScript);
//                } catch (ScriptException e) {
//                    LOG.error("Script error {}", e);
//                }
                LOG.info("The script value {}", jsScript);
            }
        }

        @Override
        public void beginInstanceDeclarationElement(Class<?> type) {

        }

        @Override
        public void beginUnknownTypeElement(String name) {
            LOG.info("Reading unknown type element {}", name);
        }

        @Override
        public void beginIncludeElement() {

        }

        @Override
        public void beginReferenceElement() {

        }

        @Override
        public void beginCopyElement() {

        }

        @Override
        public void beginRootElement() {

        }

        @Override
        public void beginPropertyElement(String name, Class<?> sourceType) {

        }

        @Override
        public void beginUnknownStaticPropertyElement(String name) {

        }

        @Override
        public void beginScriptElement() {

        }

        @Override
        public void beginDefineElement() {

        }

        @Override
        public void readInternalAttribute(String name, String value) {
            LOG.info("Reading property {} with value {}", name, value);
        }

        @Override
        public void readPropertyAttribute(String name, Class<?> sourceType, String value) {
            LOG.info("Reading property {} with value {}", name, value);
        }

        @Override
        public void readUnknownStaticPropertyAttribute(String name, String value) {
            LOG.info("Reading unknown static property {} with value {}", name, value);
        }

        @Override
        public void readEventHandlerAttribute(String name, String value) {
            LOG.info("Reading by event handler attribute {} with value {}", name, value);
        }

        private String getResultAsString(InputStream inputStream) {
            Scanner s = new Scanner(inputStream).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }
}
