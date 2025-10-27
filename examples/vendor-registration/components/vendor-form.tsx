'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { validateEmail, validatePhone, validateRegistrationNumber } from '@/lib/validation';
import { CreateVendorRequest } from '@/types/vendor';

/**
 * フォームエラー
 */
interface FormErrors {
  companyName?: string;
  registrationNumber?: string;
  address?: string;
  contactName?: string;
  contactEmail?: string;
  contactPhone?: string;
  businessCategory?: string;
}

/**
 * VendorFormコンポーネントのプロパティ
 */
export interface VendorFormProps {
  /** 初期値（編集時に使用） */
  initialValues?: Partial<CreateVendorRequest>;
  /** フォーム送信時のコールバック */
  onSubmit: (data: CreateVendorRequest) => Promise<void>;
  /** 送信ボタンのラベル */
  submitLabel?: string;
  /** キャンセルボタンのコールバック */
  onCancel?: () => void;
}

/**
 * 取引先申請フォームコンポーネント
 *
 * 取引先の情報を入力するフォームです。
 * - バリデーション付き
 * - エラーメッセージ表示
 * - ローディング状態
 *
 * @example
 * ```tsx
 * <VendorForm
 *   onSubmit={async (data) => {
 *     await createVendor(data);
 *   }}
 *   submitLabel="作成"
 * />
 * ```
 */
export function VendorForm({
  initialValues = {},
  onSubmit,
  submitLabel = '保存',
  onCancel,
}: VendorFormProps) {
  const [formData, setFormData] = useState<CreateVendorRequest>({
    companyName: initialValues.companyName || '',
    registrationNumber: initialValues.registrationNumber || '',
    address: initialValues.address || '',
    contactName: initialValues.contactName || '',
    contactEmail: initialValues.contactEmail || '',
    contactPhone: initialValues.contactPhone || '',
    businessCategory: initialValues.businessCategory || '卸売業',
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);

  /**
   * フォームバリデーション
   */
  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // 会社名
    if (!formData.companyName.trim()) {
      newErrors.companyName = '会社名を入力してください';
    }

    // 法人番号
    if (!formData.registrationNumber.trim()) {
      newErrors.registrationNumber = '法人番号を入力してください';
    } else if (!validateRegistrationNumber(formData.registrationNumber)) {
      newErrors.registrationNumber = '法人番号は13桁の数字で入力してください';
    }

    // 住所
    if (!formData.address.trim()) {
      newErrors.address = '住所を入力してください';
    }

    // 担当者名
    if (!formData.contactName.trim()) {
      newErrors.contactName = '担当者名を入力してください';
    }

    // メールアドレス
    if (!formData.contactEmail.trim()) {
      newErrors.contactEmail = 'メールアドレスを入力してください';
    } else if (!validateEmail(formData.contactEmail)) {
      newErrors.contactEmail = '有効なメールアドレスを入力してください';
    }

    // 電話番号
    if (!formData.contactPhone.trim()) {
      newErrors.contactPhone = '電話番号を入力してください';
    } else if (!validatePhone(formData.contactPhone)) {
      newErrors.contactPhone = '有効な電話番号を入力してください（例: 03-1234-5678）';
    }

    // 事業カテゴリー
    if (!formData.businessCategory.trim()) {
      newErrors.businessCategory = '事業カテゴリーを選択してください';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * フォーム送信
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setLoading(true);
    try {
      await onSubmit(formData);
    } catch (error) {
      console.error('Form submission error:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * フィールド変更時
   */
  const handleChange = (field: keyof CreateVendorRequest, value: string) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
    // エラーをクリア
    if (errors[field]) {
      setErrors((prev) => ({
        ...prev,
        [field]: undefined,
      }));
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <Card>
        <CardHeader>
          <CardTitle>会社情報</CardTitle>
          <CardDescription>取引先の基本情報を入力してください</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* 会社名 */}
          <div className="space-y-2">
            <Label htmlFor="companyName">
              会社名 <span className="text-destructive">*</span>
            </Label>
            <Input
              id="companyName"
              value={formData.companyName}
              onChange={(e) => handleChange('companyName', e.target.value)}
              placeholder="株式会社サンプル"
              disabled={loading}
            />
            {errors.companyName && (
              <p className="text-sm text-destructive">{errors.companyName}</p>
            )}
          </div>

          {/* 法人番号 */}
          <div className="space-y-2">
            <Label htmlFor="registrationNumber">
              法人番号 <span className="text-destructive">*</span>
            </Label>
            <Input
              id="registrationNumber"
              value={formData.registrationNumber}
              onChange={(e) => handleChange('registrationNumber', e.target.value)}
              placeholder="1234567890123"
              maxLength={13}
              disabled={loading}
            />
            <p className="text-xs text-muted-foreground">13桁の数字を入力してください</p>
            {errors.registrationNumber && (
              <p className="text-sm text-destructive">{errors.registrationNumber}</p>
            )}
          </div>

          {/* 住所 */}
          <div className="space-y-2">
            <Label htmlFor="address">
              住所 <span className="text-destructive">*</span>
            </Label>
            <Input
              id="address"
              value={formData.address}
              onChange={(e) => handleChange('address', e.target.value)}
              placeholder="東京都千代田区丸の内1-1-1"
              disabled={loading}
            />
            {errors.address && (
              <p className="text-sm text-destructive">{errors.address}</p>
            )}
          </div>

          {/* 事業カテゴリー */}
          <div className="space-y-2">
            <Label htmlFor="businessCategory">
              事業カテゴリー <span className="text-destructive">*</span>
            </Label>
            <Select
              id="businessCategory"
              value={formData.businessCategory}
              onChange={(e) => handleChange('businessCategory', e.target.value)}
              disabled={loading}
            >
              <option value="卸売業">卸売業</option>
              <option value="小売業">小売業</option>
              <option value="製造業">製造業</option>
              <option value="サービス業">サービス業</option>
              <option value="建設業">建設業</option>
              <option value="その他">その他</option>
            </Select>
            {errors.businessCategory && (
              <p className="text-sm text-destructive">{errors.businessCategory}</p>
            )}
          </div>
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>担当者情報</CardTitle>
          <CardDescription>取引先の担当者情報を入力してください</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* 担当者名 */}
          <div className="space-y-2">
            <Label htmlFor="contactName">
              担当者名 <span className="text-destructive">*</span>
            </Label>
            <Input
              id="contactName"
              value={formData.contactName}
              onChange={(e) => handleChange('contactName', e.target.value)}
              placeholder="山田太郎"
              disabled={loading}
            />
            {errors.contactName && (
              <p className="text-sm text-destructive">{errors.contactName}</p>
            )}
          </div>

          {/* メールアドレス */}
          <div className="space-y-2">
            <Label htmlFor="contactEmail">
              メールアドレス <span className="text-destructive">*</span>
            </Label>
            <Input
              id="contactEmail"
              type="email"
              value={formData.contactEmail}
              onChange={(e) => handleChange('contactEmail', e.target.value)}
              placeholder="yamada@example.com"
              disabled={loading}
            />
            {errors.contactEmail && (
              <p className="text-sm text-destructive">{errors.contactEmail}</p>
            )}
          </div>

          {/* 電話番号 */}
          <div className="space-y-2">
            <Label htmlFor="contactPhone">
              電話番号 <span className="text-destructive">*</span>
            </Label>
            <Input
              id="contactPhone"
              type="tel"
              value={formData.contactPhone}
              onChange={(e) => handleChange('contactPhone', e.target.value)}
              placeholder="03-1234-5678"
              disabled={loading}
            />
            <p className="text-xs text-muted-foreground">
              ハイフン区切りで入力してください（例: 03-1234-5678）
            </p>
            {errors.contactPhone && (
              <p className="text-sm text-destructive">{errors.contactPhone}</p>
            )}
          </div>
        </CardContent>
      </Card>

      {/* アクションボタン */}
      <div className="mt-6 flex justify-end gap-4">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>
            キャンセル
          </Button>
        )}
        <Button type="submit" disabled={loading}>
          {loading ? '送信中...' : submitLabel}
        </Button>
      </div>
    </form>
  );
}
