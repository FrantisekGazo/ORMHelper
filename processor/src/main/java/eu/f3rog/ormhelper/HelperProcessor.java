package eu.f3rog.ormhelper;

import com.google.auto.service.AutoService;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.code.Symbol;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class HelperProcessor extends AbstractProcessor {

    private static final String GET_DB_CONFIG_FILE_METHOD_NAME = "getDatabaseConfigFile";
    private static final String MAIN_METHOD_NAME = "main";
    private static final String DB_CONFIG_DIR_NAME = "db_config";

    private Messager mMessager;
    private Filer mFiler;
    private final HashMap<ClassName, ClassName> mIdClassNames = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new HashSet<>();
        annotations.add(Helper.class.getCanonicalName());
        annotations.add(OnUpgrade.class.getCanonicalName());
        annotations.add(DatabaseTable.class.getCanonicalName());
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeElement, List<ExecutableElement>> helpers = new HashMap<>();

        // Find all ID classes for db tables
        Set<? extends Element> dbTableClasses = roundEnv.getElementsAnnotatedWith(DatabaseTable.class);
        for (Element dbTableClass : dbTableClasses) {
            saveIdClassForTable((TypeElement) dbTableClass);
        }
        dbTableClasses = null;

        // put all helper classes
        Set<? extends Element> helperClasses = roundEnv.getElementsAnnotatedWith(Helper.class);
        for (Element helperClass : helperClasses) {
            if (!checkHelperClass((TypeElement) helperClass)) {
                return false;
            }
            helpers.put((TypeElement) helperClass, new ArrayList<ExecutableElement>());
        }
        helperClasses = null;

        // add onUpgrade methods
        Set<? extends Element> onUpgradeMethods = roundEnv.getElementsAnnotatedWith(OnUpgrade.class);
        for (Element onUpgradeMethod : onUpgradeMethods) {
            List<ExecutableElement> helperOnUpMethods = helpers.get((TypeElement) onUpgradeMethod.getEnclosingElement());
            if (helperOnUpMethods == null) {
                error(onUpgradeMethod, "Method annotated with @%s must be inside class annotated with @%s.",
                        OnUpgrade.class.getSimpleName(), Helper.class.getSimpleName());
                return false;
            }
            if (!checkOnUpMethod((ExecutableElement) onUpgradeMethod)) {
                return false;
            }
            helperOnUpMethods.add((ExecutableElement) onUpgradeMethod);
        }
        onUpgradeMethods = null;

        // generate helper classes
        for (Map.Entry<TypeElement, List<ExecutableElement>> helper : helpers.entrySet()) {
            generateHelper(helper.getKey(), helper.getValue());

            // generate config util class if necessary
            if (helper.getKey().getAnnotation(Helper.class).withConfigUtil()) {
                generateConfigUtil(helper.getKey());
            }
        }

        return false;
    }

    private void saveIdClassForTable(TypeElement dbTableClass) {
        TypeElement superClass = dbTableClass;
        while (superClass != null && !ClassName.get(superClass).equals(ClassName.get(Object.class))) {
            // look for id field
            for (Element enclosed : superClass.getEnclosedElements()) {
                if (enclosed.getKind() == ElementKind.FIELD) {
                    DatabaseField a = enclosed.getAnnotation(DatabaseField.class);
                    if (a != null && (a.id() || a.generatedId())) {
                        // support primitive types
                        Class idClass = null;
                        switch (enclosed.asType().getKind()) {
                            case INT:
                                idClass = Integer.class;
                                break;
                            case LONG:
                                idClass = Long.class;
                                break;
                            case DOUBLE:
                                idClass = Double.class;
                                break;
                            case FLOAT:
                                idClass = Float.class;
                                break;
                            default: // if object
                                break;
                        }
                        // save to map
                        ClassName dbTableClassName = (ClassName) ClassName.get(dbTableClass.asType());
                        ClassName dbFieldClassName = (idClass != null) ? ClassName.get(idClass) : (ClassName) ClassName.get(enclosed.asType());
                        mIdClassNames.put(dbTableClassName, dbFieldClassName);
                        return;
                    }
                }
            }
            // get super class
            superClass = (TypeElement) ((Symbol.ClassSymbol) superClass).getSuperclass().asElement();
        }
    }

    private boolean checkHelperClass(TypeElement helperClass) {
        // has to have empty constructor
        boolean hasEmptyConstructor = false;
        for (Element e : helperClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.CONSTRUCTOR && ((ExecutableElement) e).getParameters().isEmpty()) {
                hasEmptyConstructor = true;
                break;
            }
        }
        if (!hasEmptyConstructor) {
            error(helperClass, "Class annotated with @%s must have empty constructor.", Helper.class.getSimpleName());
            return false;
        }

        // all helper tables has to be annotated with @DatabaseTable
        List<ClassName> tableClassNames = ProcessorUtils.getParamClasses(helperClass, new ProcessorUtils.IGetter<Class<?>[]>() {
            @Override
            public Class<?>[] get(Element element) {
                return element.getAnnotation(Helper.class).tables();
            }
        });
        if (!mIdClassNames.keySet().containsAll(tableClassNames)) {
            error(helperClass, "@%s tables must contain classes annotated with @%s with one id field annotated with @%s (can be inherited).",
                    Helper.class.getSimpleName(), DatabaseTable.class.getSimpleName(), DatabaseField.class.getSimpleName());
            return false;
        }

        return true;
    }

    private boolean checkOnUpMethod(ExecutableElement onUpgradeMethod) {
        // has to be public void
        for (Modifier modifier : onUpgradeMethod.getModifiers()) {
            if (modifier.equals(Modifier.STATIC)
                    || modifier.equals(Modifier.ABSTRACT)
                    || modifier.equals(Modifier.PRIVATE)
                    || modifier.equals(Modifier.PROTECTED)) {
                error(onUpgradeMethod, "Method annotated with @%s must be public non-static method returning void.",
                        OnUpgrade.class.getSimpleName());
                return false;
            }
        }
        // has to have 2 parameters
        List<? extends VariableElement> params = ((ExecutableElement) onUpgradeMethod).getParameters();
        if (params.size() != 2) {
            error(onUpgradeMethod, "Method annotated with @%s must have 2 parameters -> (%s database, %s connectionSource).",
                    OnUpgrade.class.getSimpleName(), EClass.SQLiteDatabase.getName().simpleName(), EClass.ConnectionSource.getName().simpleName());
            return false;
        }
        // check 1st parameter class
        if (!ClassName.get(params.get(0).asType()).equals(EClass.SQLiteDatabase.getName())) {
            error(onUpgradeMethod, "Method annotated with @%s must have 1st parameter of type %s.",
                    OnUpgrade.class.getSimpleName(), EClass.SQLiteDatabase.getName().simpleName());
            return false;
        }
        // check 2nd parameter class
        if (!ClassName.get(params.get(1).asType()).equals(EClass.ConnectionSource.getName())) {
            error(onUpgradeMethod, "Method annotated with @%s must have 2nd parameter of type %s.",
                    OnUpgrade.class.getSimpleName(), EClass.ConnectionSource.getName().simpleName());
            return false;
        }

        return true;
    }

    private void generateHelper(final TypeElement helperClass, final List<ExecutableElement> onUpgradeMethods) {
        Helper helperAnnotation = helperClass.getAnnotation(Helper.class);

        final TypeSpec.Builder helper = TypeSpec.classBuilder(getHelperClassName(helperAnnotation.name()));
        helper.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        helper.superclass(EClass.OrmLiteSqliteOpenHelper.getName());

        // add constructor
        helper.addMethod(generateHelperConstructor(helperAnnotation));
        // add DAOs
        List<ClassName> tableClassNames = ProcessorUtils.getParamClasses(helperClass, new ProcessorUtils.IGetter<Class<?>[]>() {
            @Override
            public Class<?>[] get(Element element) {
                return helperClass.getAnnotation(Helper.class).tables();
            }
        });
        for (ClassName tableClassName : tableClassNames) {
            helper.addField(generateDaoField(tableClassName));
        }
        // implement onCreate method
        helper.addMethod(generateOnCreateMethod(helperClass, tableClassNames));
        // implement onUpgrade method
        boolean dropOnUp = helperAnnotation.dropOnUpgrade();
        helper.addMethod(generateOnUpgradeMethod(helperClass, tableClassNames, onUpgradeMethods, dropOnUp));
        if (dropOnUp) {
            // implement drop method
            helper.addMethod(generateDropMethod(helperClass, tableClassNames));
        }
        // implement clear method
        helper.addMethod(generateClearMethod(helperClass, tableClassNames));
        // implement close method
        helper.addMethod(generateCloseMethod(tableClassNames));
        // implement getters
        for (ClassName tableClassName : tableClassNames) {
            helper.addMethod(generateDaoGetter(tableClassName));
        }

        // create file
        String packageName = helperClass.getQualifiedName().toString();
        packageName = packageName.substring(0, packageName.lastIndexOf("."));
        JavaFile javaFile = JavaFile.builder(packageName, helper.build())
                .build();
        try {
            javaFile.writeTo(mFiler);
            javaFile.writeTo(System.out);
            System.out.println(String.format("%s successfully generated for %s.", getHelperClassName(helperAnnotation.name()), helperClass.getSimpleName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MethodSpec generateHelperConstructor(Helper helperAnnotation) {
        MethodSpec.Builder method = MethodSpec.constructorBuilder();
        method.addModifiers(Modifier.PUBLIC);
        method.addParameter(EClass.Context.getName(), "context");
        if (!helperAnnotation.withConfigUtil()) {
            method.addStatement("super(context, $S, null, $L)",
                    helperAnnotation.name() + ".db", helperAnnotation.version());
        } else {
            method.addStatement("super(context, $S, null, $L, $N.$N())",
                    helperAnnotation.name() + ".db", helperAnnotation.version(), getConfigUtilClassName(helperAnnotation.name()), GET_DB_CONFIG_FILE_METHOD_NAME);
        }
        return method.build();
    }

    private FieldSpec generateDaoField(ClassName tableClassName) {
        return FieldSpec.builder(getDaoType(tableClassName), getDaoName(tableClassName))
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    private MethodSpec generateOnCreateMethod(TypeElement helperClass, List<ClassName> tableClassNames) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("onCreate");
        method.addAnnotation(Override.class);
        method.addModifiers(Modifier.PUBLIC);
        method.addParameter(EClass.SQLiteDatabase.getName(), "database");
        method.addParameter(EClass.ConnectionSource.getName(), "connectionSource");

        method.addStatement("$T.i($T.class.getName(), $S)", EClass.Log.getName(), helperClass, "onCreate");
        method.beginControlFlow("try");
        for (ClassName tableClassName : tableClassNames) {
            method.addStatement("$T.createTable(connectionSource, $T.class)", EClass.TableUtils.getName(), tableClassName);
        }
        method.endControlFlow();

        method.beginControlFlow("catch ($T e)", ClassName.get(SQLException.class));
        method.addStatement("$T.e($T.class.getName(), $S, e)", EClass.Log.getName(), helperClass, "Can't create database tables.");
        method.addStatement("throw new $N(e)", RuntimeException.class.getSimpleName());
        method.endControlFlow();

        return method.build();
    }

    private MethodSpec generateClearMethod(TypeElement helperClass, List<ClassName> tableClassNames) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("clearTables");
        method.addModifiers(Modifier.PUBLIC);

        method.addStatement("$T.i($T.class.getName(), $S)", EClass.Log.getName(), helperClass, "clearTables");
        method.beginControlFlow("try");
        method.addStatement("connectionSource.getReadWriteConnection()");
        for (ClassName tableClassName : tableClassNames) {
            method.addStatement("$T.clearTable(connectionSource, $T.class)", EClass.TableUtils.getName(), tableClassName);
        }
        method.endControlFlow();

        method.beginControlFlow("catch ($T e)", ClassName.get(SQLException.class));
        method.addStatement("$T.e($T.class.getName(), $S, e)", EClass.Log.getName(), helperClass, "Can't clear database tables.");
        method.endControlFlow();

        return method.build();
    }

    private MethodSpec generateDropMethod(TypeElement helperClass, List<ClassName> tableClassNames) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("dropTables");
        method.addModifiers(Modifier.PRIVATE);
        method.addParameter(EClass.SQLiteDatabase.getName(), "database");
        method.addParameter(EClass.ConnectionSource.getName(), "connectionSource");

        method.addStatement("$T.i($T.class.getName(), $S)", EClass.Log.getName(), helperClass, "dropTables");
        method.beginControlFlow("try");
        for (ClassName tableClassName : tableClassNames) {
            method.addStatement("$T.dropTable(connectionSource, $T.class, true)", EClass.TableUtils.getName(), tableClassName);
        }
        method.endControlFlow();

        method.beginControlFlow("catch ($T e)", ClassName.get(SQLException.class));
        method.addStatement("$T.e($T.class.getName(), $S, e)", EClass.Log.getName(), helperClass, "Can't drop database tables.");
        method.endControlFlow();

        return method.build();
    }

    private MethodSpec generateOnUpgradeMethod(TypeElement helperClass, List<ClassName> tableClassNames, List<ExecutableElement> onUpgradeMethods, boolean clearOnUpgrade) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("onUpgrade");
        method.addAnnotation(Override.class);
        method.addModifiers(Modifier.PUBLIC);
        method.addParameter(EClass.SQLiteDatabase.getName(), "database");
        method.addParameter(EClass.ConnectionSource.getName(), "connectionSource");
        method.addParameter(int.class, "oldVersion");
        method.addParameter(int.class, "newVersion");

        if (clearOnUpgrade) {
            method.addStatement("dropTables(database, connectionSource)");
            method.addStatement("onCreate(database, connectionSource)");
        } else {
            method.addStatement("$T upgradeHelper = new $T()", helperClass, helperClass);
            method.addStatement("int version = oldVersion");

            // use onUpgradeMethods
            Collections.sort(onUpgradeMethods, new OnUpMethodComparator());
            OnUpgrade onUp;
            ExecutableElement onUpMethod;
            for (int i = 0; i < onUpgradeMethods.size(); i++) {
                onUpMethod = onUpgradeMethods.get(i);
                onUp = onUpMethod.getAnnotation(OnUpgrade.class);
                if (onUp.from() == OnUpgrade.UNDEFINED) {
                    method.beginControlFlow("if (version <= $L)", onUp.to());
                } else {
                    method.beginControlFlow("if (version == $L)", onUp.from());
                }
                method.addStatement("upgradeHelper.$N(database, connectionSource)", onUpMethod.getSimpleName());
                method.addStatement("version = $L", onUp.to());
                method.endControlFlow();
            }
        }

        return method.build();
    }

    private MethodSpec generateCloseMethod(List<ClassName> tableClassNames) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("close");
        method.addAnnotation(Override.class);
        method.addModifiers(Modifier.PUBLIC);
        // call super
        method.addStatement("super.close()");
        for (ClassName tableClassName : tableClassNames) {
            method.addStatement("$N = null", getDaoName(tableClassName));
        }
        return method.build();
    }

    private MethodSpec generateDaoGetter(ClassName tableClassName) {
        String dao = getDaoName(tableClassName);

        MethodSpec.Builder method = MethodSpec.methodBuilder(getDaoGetterName(tableClassName));
        method.addModifiers(Modifier.PUBLIC);
        method.returns(getDaoType(tableClassName));
        method.addException(ClassName.get(SQLException.class));

        method.beginControlFlow("if ($N == null) ", dao);
        method.addStatement("$N = getDao($T.class)", dao, tableClassName);
        method.endControlFlow();
        method.addStatement("return $N", dao);

        return method.build();
    }

    public ParameterizedTypeName getDaoType(ClassName tableClassName) {
        return ParameterizedTypeName.get(EClass.Dao.getName(), tableClassName, getIdClassName(tableClassName));
    }

    private ClassName getIdClassName(ClassName tableClassName) {
        return mIdClassNames.get(tableClassName);
    }

    private String getHelperClassName(String databaseName) {
        return String.format("%sDatabaseHelper", formatDbName(databaseName));
    }

    private String getConfigUtilClassName(String databaseName) {
        return String.format("%sDatabaseConfigUtil", formatDbName(databaseName));
    }

    private String formatDbName(String databaseName) {
        databaseName = databaseName.replaceAll("[^A-Za-z0-9]", "");
        return databaseName.substring(0, 1).toUpperCase() + databaseName.substring(1);
    }

    private String getDaoName(ClassName tableClassName) {
        return String.format("m%sDao", tableClassName.simpleName());
    }

    private String getDaoGetterName(ClassName tableClassName) {
        return String.format("get%sDao", tableClassName.simpleName());
    }

    private void generateConfigUtil(final TypeElement helperClass) {
        final Helper helperAnnotation = helperClass.getAnnotation(Helper.class);

        final TypeSpec.Builder configUtil = TypeSpec.classBuilder(getConfigUtilClassName(helperAnnotation.name()));
        configUtil.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        configUtil.superclass(EClass.OrmLiteConfigUtil.getName());

        // add table list field
        List<ClassName> tableClassNames = ProcessorUtils.getParamClasses(helperClass, new ProcessorUtils.IGetter<Class<?>[]>() {
            @Override
            public Class<?>[] get(Element element) {
                return helperAnnotation.tables();
            }
        });
        FieldSpec tableListField = generateTableListField(tableClassNames);
        configUtil.addField(tableListField);
        // add config file getter method
        configUtil.addMethod(generateGetConfigFileMethod(helperAnnotation.name()));
        // add main method
        configUtil.addMethod(generateMainConfigMethod(tableListField));

        // create file
        String packageName = helperClass.getQualifiedName().toString();
        packageName = packageName.substring(0, packageName.lastIndexOf("."));
        JavaFile javaFile = JavaFile.builder(packageName, configUtil.build())
                .build();
        try {
            javaFile.writeTo(mFiler);
            javaFile.writeTo(System.out);
            System.out.println(String.format("%s successfully generated for %s.", getConfigUtilClassName(helperAnnotation.name()), helperClass.getSimpleName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private FieldSpec generateTableListField(List<ClassName> tableClassNames) {
        FieldSpec.Builder field = FieldSpec.builder(Class[].class, "sTableClasses", Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC);
        String format = "new Class[] {\n";
        Object args[] = new Object[tableClassNames.size()];
        for (int i = 0; i < tableClassNames.size(); i++) {
            format += "\t$T.class,\n";
            args[i] = tableClassNames.get(i);
        }
        format += "}";
        field.initializer(format, args);
        return field.build();
    }

    private MethodSpec generateGetConfigFileMethod(String databaseName) {
        return MethodSpec.methodBuilder(GET_DB_CONFIG_FILE_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(File.class)
                .addStatement("File dir = new File($S)", DB_CONFIG_DIR_NAME)
                .addStatement("dir.mkdir()")
                .addStatement("File configFile = new File(dir, $S)", getConfigFileName(databaseName))
                .beginControlFlow("if (!configFile.exists())")
                .beginControlFlow("try")
                .addStatement("configFile.createNewFile()")
                .endControlFlow()
                .beginControlFlow("catch ($T e)", IOException.class)
                .addStatement("e.printStackTrace()")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return configFile")
                .build();
    }

    private String getConfigFileName(String databaseName) {
        return String.format("%s_config.txt", databaseName);
    }

    private MethodSpec generateMainConfigMethod(FieldSpec tableListField) {
        return MethodSpec.methodBuilder(MAIN_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String[].class, "args")
                .addException(IOException.class)
                .addException(SQLException.class)
                .addStatement("writeConfigFile($N(), $N)", GET_DB_CONFIG_FILE_METHOD_NAME, tableListField)
                .build();
    }

    private void error(Element e, String msg, Object... args) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }
}
