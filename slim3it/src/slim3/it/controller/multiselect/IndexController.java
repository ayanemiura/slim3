package slim3.it.controller.multiselect;

import java.util.logging.Logger;

import org.slim3.controller.JDOController;
import org.slim3.controller.Navigation;

public class IndexController extends JDOController {

    @SuppressWarnings("unused")
    private static final Logger logger =
        Logger.getLogger(IndexController.class.getName());

    @Override
    public Navigation run() {
        if (isGet()) {
            requestScope("aaaArray", new String[] { "2" });
        }
        return forward("index.jsp");
    }
}