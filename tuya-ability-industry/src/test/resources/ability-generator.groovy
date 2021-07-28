import groovy.io.FileType

import java.util.regex.Pattern

def userDir = System.getProperty('user.dir')
def moduleRoot = new File(userDir)
while (moduleRoot.path.contains('src')) {
    moduleRoot = moduleRoot.getParentFile()
}
println moduleRoot

/**
 * !!! 运行该脚本之前，请先提交好代码
 *
 * 该脚本的功能：
 * 扫描项目中Connector.java结尾的文件，生成对应的Ability.java，
 * 并且修改Connector.java(extends生成的接口，添加@Override注解)
 *
 * 重复执行结果幂等
 * */
new File(moduleRoot, 'src/main/java').eachFileRecurse(FileType.FILES) {
//    if (it.name != 'AclGrantConnector.java') {
//        return
//    }
    //匹配 ***Connector.java
    if (it.name.endsWith('Connector.java')) {
        //创建对应的ability目录
        def abilityDir = new File(it.parentFile.parentFile, 'ability')
        if (!abilityDir.exists()) {
            abilityDir.mkdirs()
        }
        //根据文本内容，收集Connector的信息
        def connector = [
                simpleName: '',//简单类名
                name: it.name.split(/\./)[0],//简单类名，和simpleName一样啊
                code: '',//全部代码
                pkg : '',//包
                imports:[],//导入的信息
                methods:[:],
                body:''//类的body
        ]
        connector.simpleName = connector.name.split(/\./)[-1]
        connector.code = it.text
        connector.pkg = (abilityDir.absolutePath - moduleRoot.absolutePath - '/src/main/java/')
                .replaceAll('/', '.')
        connector.imports = connector.code.split(/\n/).findAll{
            it.matches(/^\s*import\s+.*$/)
        }

        connector.body = connector.code[connector.code.indexOf("{")+1..(connector.code.lastIndexOf("}")-1)]

        //生成对应的Ability的信息
        def ability = [
                simpleName:'',
                name: connector.name.replace('Connector', 'Ability'),
                code:'',
                pkg:(connector.pkg.split(/\./)[0..-2]<<'ability').join('.'),
                imports: [],
                body: ''
        ]
        ability.simpleName = ability.name.split(/\./)[-1]
        ability.imports = connector.imports.findAll{
            String im->
                !im.contains('com.tuya.connector.api.annotations')
                && !im.contains("import ${ability.pkg}.${ability.simpleName};")
        }
        //去掉所有的注解
        ability.body = connector.body.replaceAll(/@[a-zA-Z0-9\.]+(\([\S]+\))?/,'')

        //生成新的Connector的代码
        def connectorCodeLines = connector.code.split(/\n/)
        def firstLine = connectorCodeLines[0]
        def restLines = connectorCodeLines[1..-1].findAll{
            !it.contains("import ${ability.pkg}.${ability.simpleName};")
        }.collect{
            it.replaceAll("(${connector.simpleName})\\s*?\\{","\$1 extends ${ability.simpleName}{")
        }.join('\n')
        //先去掉@Override
        .replaceAll(Pattern.compile(/@Override\s*/,Pattern.MULTILINE),"")
        //再加上@Override
        .replaceAll(/(?<httpMethod>@(GET|POST|PUT|DELETE)\s*\(.*?\))/,'${httpMethod}\n    @Override ')

        def connectorNewCode = ([firstLine,"\nimport ${ability.pkg}.${ability.simpleName};",restLines]).join('\n')
        //合并空行
                .replaceAll(Pattern.compile(/(\s*\r?\n){3,}/,Pattern.MULTILINE),'\n\n')
        //输出Ability的代码
        new File(abilityDir,ability.simpleName+'.java').text = """\
package ${ability.pkg};

${ability.imports.join('\n')}

public interface ${ability.simpleName}{
${ability.body}
}
""".replaceAll(Pattern.compile(/(\s*\r?\n){3,}/,Pattern.MULTILINE),'\n\n')

        //输出新的Connector的代码
        it.text = connectorNewCode
    }
}