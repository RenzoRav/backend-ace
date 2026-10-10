package br.com.florir.service;

public final class PhoneNumbers {
  private PhoneNumbers() {}

  public static String normalize(String value) {
    if (value == null) throw new IllegalArgumentException("Telefone obrigatório");
    String digits = value.replaceAll("[^0-9]", "");
    if ((digits.length() == 12 || digits.length() == 13) && digits.startsWith("55"))
      digits = digits.substring(2);
    if (!digits.matches("[1-9][0-9]{9,10}"))
      throw new IllegalArgumentException("Telefone brasileiro inválido");
    return digits;
  }
}
