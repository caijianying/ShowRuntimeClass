import com.sun.tools.attach.VirtualMachine;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * @author caijy
 * @description
 * @date 2024/3/26 星期二 3:33 下午
 */
public class PluginTest {

    @Test
    public void testRead(){
        String errorPath2 = "/Users/jianyingcai/Library/Application Support/JetBrains/IntelliJIdea2021.1/plugins/ShowRuntimeClass/lib/plugin-agent-20240326-all.jar";
        String errorPath = "/Users/jianyingcai/Library/Application Support/JetBrains/IntelliJIdea2021.1/plugins/ShowRuntimeClass/lib/monitor-agent-test-20240326-all.jar";
        String path = "/Users/jianyingcai/IdeaProjects/practice/Service-invocation-monitor/monitor-agent-test/build/libs/monitor-agent-test-20240326-all.jar";
        String pid = "97071";
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(pid);
            vm.loadAgent(errorPath2,"{}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
