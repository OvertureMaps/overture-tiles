public class Base implements OvertureProfile.Theme {

  @Override
  public String name() {
    return "base";
  }

  public static void main(String[] args) throws Exception {
    OvertureProfile.run(Arguments.fromArgsOrConfigFile(args), new Base());
  }
}

