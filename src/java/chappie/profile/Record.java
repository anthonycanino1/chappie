package chappie.profile;

public interface Record {
  public static Record of(Object obj) {
    return new ObjectRecord(obj);
  }
}

class ObjectRecord implements Record {
  Object value;

  public ObjectRecord(Object obj) {
    value = obj;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
