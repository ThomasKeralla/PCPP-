import java.util.function.IntToDoubleFunction;

public abstract class Benchmarkable implements IntToDoubleFunction {
  public void setup() { }
  public abstract double applyAsDouble(int i);
}
