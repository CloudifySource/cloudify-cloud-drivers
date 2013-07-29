public class NiceOutput {

    private static final String TAB = "    ";
    static int tabCounts = 0;

    /**
     * Nice Output for object using ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
     */
    static public void toString(Object object) {
        if (object == null)
            return;
        StringBuilder sb = new StringBuilder();
        String string = object.toString();
        for (int i = 0; i < string.length(); i++) {
            char charAt = string.charAt(i);
            switch (charAt) {
            case '{':
            case '[':
                sb.append(charAt).append("\r\n");
                tabCounts++;
                addTab(sb);
                break;
            case ',':
                sb.append(",\r\n");
                addTab(sb);
                break;
            case ']':
            case '}':
                sb.append("\r\n");
                tabCounts--;
                addTab(sb).append(charAt);
                break;
            case ' ':
            case '\r':
                break;
            case '\n':
                sb.append("\r\n");
                addTab(sb);
                break;
            default:
                sb.append(charAt);
            }
        }

        System.out.println(sb.toString());
    }

    private static StringBuilder addTab(StringBuilder sb) {
        for (int tab = 0; tab < tabCounts; tab++) {
            sb.append(TAB);
        }
        return sb;
    }
}
