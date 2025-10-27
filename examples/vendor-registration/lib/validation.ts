import { CreateVendorRequest } from '@/types/vendor';

/**
 * バリデーションエラー
 */
export interface ValidationError {
  field: string;
  message: string;
}

/**
 * バリデーション結果
 */
export interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
}

/**
 * メールアドレスのバリデーション
 */
export function validateEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * 電話番号のバリデーション（日本の形式）
 */
export function validatePhone(phone: string): boolean {
  // ハイフンあり・なし両方対応
  const phoneRegex = /^0\d{1,4}-?\d{1,4}-?\d{4}$/;
  return phoneRegex.test(phone);
}

/**
 * 法人番号のバリデーション（13桁）
 */
export function validateRegistrationNumber(registrationNumber: string): boolean {
  const regNumRegex = /^\d{13}$/;
  return regNumRegex.test(registrationNumber);
}

/**
 * 必須フィールドのバリデーション
 */
export function validateRequired(value: string, fieldName: string): ValidationError | null {
  if (!value || value.trim() === '') {
    return {
      field: fieldName,
      message: `${fieldName}は必須項目です`,
    };
  }
  return null;
}

/**
 * 取引先登録リクエストのバリデーション
 */
export function validateVendorRequest(request: CreateVendorRequest): ValidationResult {
  const errors: ValidationError[] = [];

  // 必須フィールドチェック
  const requiredFields: (keyof CreateVendorRequest)[] = [
    'companyName',
    'registrationNumber',
    'address',
    'contactName',
    'contactEmail',
    'contactPhone',
    'businessCategory',
  ];

  for (const field of requiredFields) {
    const error = validateRequired(request[field], field);
    if (error) {
      errors.push(error);
    }
  }

  // メールアドレスの形式チェック
  if (request.contactEmail && !validateEmail(request.contactEmail)) {
    errors.push({
      field: 'contactEmail',
      message: 'メールアドレスの形式が正しくありません',
    });
  }

  // 電話番号の形式チェック
  if (request.contactPhone && !validatePhone(request.contactPhone)) {
    errors.push({
      field: 'contactPhone',
      message: '電話番号の形式が正しくありません（例: 03-1234-5678）',
    });
  }

  // 法人番号の形式チェック
  if (request.registrationNumber && !validateRegistrationNumber(request.registrationNumber)) {
    errors.push({
      field: 'registrationNumber',
      message: '法人番号は13桁の数字で入力してください',
    });
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

/**
 * フィールド名の日本語表示マッピング
 */
export const FIELD_LABELS: Record<keyof CreateVendorRequest, string> = {
  companyName: '会社名',
  registrationNumber: '法人番号',
  address: '住所',
  contactName: '担当者名',
  contactEmail: '担当者メールアドレス',
  contactPhone: '担当者電話番号',
  businessCategory: '事業カテゴリー',
};

/**
 * バリデーションエラーメッセージを取得
 */
export function getErrorMessage(field: string, errors: ValidationError[]): string | undefined {
  const error = errors.find((e) => e.field === field);
  return error?.message;
}

/**
 * フォームにエラーがあるか確認
 */
export function hasErrors(errors: ValidationError[]): boolean {
  return errors.length > 0;
}
